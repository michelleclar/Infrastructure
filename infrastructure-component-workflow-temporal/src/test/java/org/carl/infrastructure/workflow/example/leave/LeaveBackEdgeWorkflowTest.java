package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * End-to-end test for {@link LeaveProcess#withRejectionLoopFlow()}. Drives the new back-edge
 * support: when the approval is rejected the workflow loops back to {@code requestLeave} (which
 * re-executes the {@code createLeaveRequest} activity), then on the second approval the workflow
 * terminates at {@code done}.
 *
 * <p>This exists to prove that, with an explicit {@code startNodeId}, a back-edge into the start
 * node is legal (the {@link org.carl.infrastructure.workflow.graph.GraphValidator} no longer
 * requires a node with zero incoming edges) and that the runtime drives the loop correctly.
 */
class LeaveBackEdgeWorkflowTest {

    private static final String TASK_QUEUE = "LEAVE_V2_BACKEDGE_TEST";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private ObjectMapper mapper;
    private AtomicInteger createLeaveRequestInvocations;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        createLeaveRequestInvocations = new AtomicInteger();
        AtomicInteger notifyManagerInvocations = new AtomicInteger();
        BusinessActivityRegistry activityRegistry =
                LeaveActivities.buildRegistry(
                        createLeaveRequestInvocations, notifyManagerInvocations);

        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);

        testEnv.start();
        client = testEnv.getWorkflowClient();
        mapper = ObjectMapperHolder.mapper();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    @Test
    @Timeout(30)
    void rejectedThenApproved_loopsBackAndFinishes() throws Exception {
        WorkflowDefinition def = LeaveProcess.withRejectionLoopFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-backedge-");
        WorkflowClient.start(workflow::execute, input);

        // First wait: workflow reaches leaveApproval and is awaiting the "approval" event.
        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("approval", decisionPayload("rejected")));

        // After rejection we loop back to requestLeave (a serviceTask), which re-invokes the
        // createLeaveRequest activity, then routes back to leaveApproval and waits again.
        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("done", result.finalNodeId());

        // The activity counter is the load-bearing assertion: it proves the back-edge fired and
        // requestLeave actually re-executed. nodeResults only keeps the latest result per node id;
        // the full visit history is now available in executionRecords.
        assertTrue(
                createLeaveRequestInvocations.get() >= 2,
                () ->
                        "expected createLeaveRequest to be re-executed via the back-edge; got "
                                + createLeaveRequestInvocations.get()
                                + " invocations");

        // executionRecords captures every visit: requestLeave x2, leaveApproval x2, done x1.
        List<org.carl.infrastructure.workflow.definition.ExecutionRecord> records =
                result.executionRecords();
        assertNotNull(records, "executionRecords should not be null");
        long requestLeaveVisits =
                records.stream().filter(r -> "requestLeave".equals(r.nodeId())).count();
        assertTrue(
                requestLeaveVisits >= 2,
                () ->
                        "expected requestLeave to appear at least twice in executionRecords; got "
                                + requestLeaveVisits);

        NodeResult approval = result.nodeResults().get("leaveApproval");
        assertNotNull(approval, "leaveApproval result should be recorded");
        assertEquals(
                Outcomes.APPROVED,
                approval.outcome(),
                "final leaveApproval outcome should be APPROVED (last write wins)");
    }

    // ---- helpers ---------------------------------------------------------------------

    private WorkflowInput newInput(WorkflowDefinition def) throws Exception {
        return WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));
    }

    private GenericWorkflow newWorkflowStub(String workflowIdPrefix) {
        return client.newWorkflowStub(
                GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowIdPrefix + UUID.randomUUID())
                        .build());
    }

    private static ObjectNode decisionPayload(String decision) {
        return JsonNodeFactory.instance.objectNode().put("decision", decision);
    }
}
