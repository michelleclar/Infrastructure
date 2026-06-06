package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end tests for {@link LeaveProcess#singleApprovalFlow()} running on Temporal's in-memory
 * test environment.
 *
 * <p>Drives the generic V2 runtime against the leave workflow definition through the approved /
 * rejected / timeout paths.
 */
class LeaveWorkflowTest {

    private static final String TASK_QUEUE = "LEAVE_V2_TEST";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        BusinessActivityRegistry activityRegistry = LeaveActivities.buildRegistry();

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
    void approvalApproved_runsToCompleted() throws Exception {
        WorkflowDefinition def = LeaveProcess.singleApprovalFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-approved-");
        WorkflowClient.start(workflow::execute, input);

        // Give the workflow a tick to reach the approvalTask waiting state, then signal approval.
        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());

        NodeResult approval = result.nodeResults().get("leaveApproval");
        assertNotNull(approval, "leaveApproval result should be recorded");
        assertEquals(Outcomes.APPROVED, approval.outcome());

        NodeResult requestLeave = result.nodeResults().get("requestLeave");
        assertNotNull(requestLeave, "requestLeave result should be recorded");
        assertEquals(Outcomes.SUCCESS, requestLeave.outcome());
    }

    @Test
    @Timeout(30)
    void approvalRejected_routesToRejectedEnd() throws Exception {
        WorkflowDefinition def = LeaveProcess.singleApprovalFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-rejected-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("approval", decisionPayload("rejected")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("rejected", result.finalNodeId());

        NodeResult approval = result.nodeResults().get("leaveApproval");
        assertNotNull(approval, "leaveApproval result should be recorded");
        assertEquals(Outcomes.REJECTED, approval.outcome());
    }

    @Test
    @Timeout(30)
    void approvalTimeout_routesToTimedOutEnd() throws Exception {
        WorkflowDefinition def = LeaveProcess.singleApprovalFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-timeout-");
        WorkflowClient.start(workflow::execute, input);

        // No signal: advance the virtual clock past the PT24H approval timeout.
        testEnv.sleep(Duration.ofHours(25));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("timedOut", result.finalNodeId());

        NodeResult approval = result.nodeResults().get("leaveApproval");
        assertNotNull(approval, "leaveApproval result should be recorded");
        assertEquals(Outcomes.TIMEOUT, approval.outcome());
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
