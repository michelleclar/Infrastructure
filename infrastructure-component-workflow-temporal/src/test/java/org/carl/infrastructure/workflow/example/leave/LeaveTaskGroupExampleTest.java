package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.carl.infrastructure.workflow.handlers.TaskGroupContract;
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
 * End-to-end tests for {@link LeaveProcess#coSignFlow()} (HR + manager co-sign via {@code
 * taskGroup} with {@code joinRule=ALL}).
 */
class LeaveTaskGroupExampleTest {

    private static final String TASK_QUEUE = "LEAVE_V2_COSIGN_TEST";

    private static final String HR_KEY = TaskGroupContract.childKey("approvals", "hrApproval");
    private static final String MANAGER_KEY =
            TaskGroupContract.childKey("approvals", "managerApproval");

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
    void bothApproved_routesToCompleted() throws Exception {
        WorkflowDefinition def = LeaveProcess.coSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-cosign-approved-");
        WorkflowClient.start(workflow::execute, input);

        // Let the workflow reach the taskGroup waiting state.
        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "approved"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());

        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "approvals taskGroup result should be recorded");
        assertEquals(Outcomes.APPROVED, approvals.outcome());

        NodeResult hr = result.nodeResults().get(HR_KEY);
        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(hr, "HR child result should be recorded under " + HR_KEY);
        assertNotNull(mgr, "Manager child result should be recorded under " + MANAGER_KEY);
        assertEquals(Outcomes.APPROVED, hr.outcome());
        assertEquals(Outcomes.APPROVED, mgr.outcome());
    }

    @Test
    @Timeout(30)
    void anyRejected_routesToRejectedEnd() throws Exception {
        WorkflowDefinition def = LeaveProcess.coSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-cosign-rejected-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "rejected"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("rejected", result.finalNodeId());

        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "approvals taskGroup result should be recorded");
        assertEquals(Outcomes.REJECTED, approvals.outcome());

        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(mgr, "Manager child result should be recorded under " + MANAGER_KEY);
        assertEquals(Outcomes.REJECTED, mgr.outcome());
    }

    /**
     * The approval handler ignores events whose {@code taskId} payload addresses a different node.
     * Here we send a signal whose taskId points at a non-existent child; the workflow must still be
     * waiting after a brief sleep, then completes once the correct taskIds arrive.
     */
    @Test
    @Timeout(30)
    void mismatchedTaskIdIgnored_workflowWaitsForCorrectTaskId() throws Exception {
        WorkflowDefinition def = LeaveProcess.coSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-cosign-taskid-mismatch-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));

        // Wrong taskId — neither HR nor manager child should accept it.
        workflow.signal(approvalSignal("approvals/unknownChild", "approved"));
        testEnv.sleep(Duration.ofSeconds(2));

        assertFalse(
                workflow.query().finished(),
                "workflow must still be running after a mismatched-taskId signal");

        // Now send the correct ones — workflow should complete.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "approved"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());
        assertEquals(Outcomes.APPROVED, result.nodeResults().get("approvals").outcome());
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

    /**
     * Build a unified "approval" signal whose payload routes to one specific taskGroup child via
     * {@link org.carl.infrastructure.workflow.handlers.ApprovalTaskHandler#PAYLOAD_TASK_ID}.
     */
    private static WorkflowEvent approvalSignal(String taskId, String decision) {
        ObjectNode payload =
                JsonNodeFactory.instance
                        .objectNode()
                        .put("taskId", taskId)
                        .put("decision", decision);
        return new WorkflowEvent("approval", payload);
    }
}
