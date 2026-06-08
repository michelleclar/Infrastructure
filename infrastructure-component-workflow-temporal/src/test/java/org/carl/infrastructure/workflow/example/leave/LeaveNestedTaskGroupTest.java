package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
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
 * End-to-end tests for nested {@code taskGroup} workflows.
 *
 * <p>Scenario: outer ALL gate (HR approval + management-layer), where management-layer is itself an
 * ANY gate (manager OR CFO).
 *
 * <pre>
 * requestLeave --(SUCCESS)--> approvals (taskGroup ALL)
 *   approvals
 *     ├── hrApproval         (approvalTask, "hr")
 *     └── managementApproval (taskGroup ANY)
 *         ├── managerApproval (approvalTask, "manager")
 *         └── cfoApproval    (approvalTask, "cfo")
 *   approvals --APPROVED--> onLeave --(SUCCESS)--> completed
 *   approvals --REJECTED--> rejected
 * </pre>
 */
class LeaveNestedTaskGroupTest {

    private static final String TASK_QUEUE = "LEAVE_NESTED_TASKGROUP_TEST";

    /** Child key for HR leaf under outer taskGroup "approvals". */
    private static final String HR_KEY =
            TaskGroupContract.childKey("approvals", "hrApproval");

    /** Child key for the nested management-layer taskGroup under "approvals". */
    private static final String MGMT_KEY =
            TaskGroupContract.childKey("approvals", "managementApproval");

    /** Child key for manager leaf under "approvals/managementApproval". */
    private static final String MANAGER_KEY =
            TaskGroupContract.childKey(MGMT_KEY, "managerApproval");

    /** Child key for CFO leaf under "approvals/managementApproval". */
    private static final String CFO_KEY =
            TaskGroupContract.childKey(MGMT_KEY, "cfoApproval");

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;

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
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    /**
     * HR approved + manager approved → entire approvals group APPROVED → completed.
     *
     * <p>The management-layer ANY gate short-circuits on manager's approval (CFO not needed).
     */
    @Test
    @Timeout(30)
    void nestedAll_anyManagerLayerApproved_routesToCompleted() throws Exception {
        WorkflowDefinition def = LeaveProcess.nestedApprovalFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));

        GenericWorkflow workflow = newWorkflowStub("nested-approved-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));

        // HR approves the outer layer.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        // Manager approves — short-circuits the inner ANY gate.
        workflow.signal(approvalSignal(MANAGER_KEY, "approved"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());

        // Outer taskGroup must have recorded APPROVED.
        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "outer approvals result must be recorded");
        assertEquals(Outcomes.APPROVED, approvals.outcome());

        // HR child must be recorded.
        NodeResult hr = result.nodeResults().get(HR_KEY);
        assertNotNull(hr, "HR result must be recorded at " + HR_KEY);
        assertEquals(Outcomes.APPROVED, hr.outcome());

        // Management-layer (nested taskGroup) result must be recorded.
        NodeResult mgmt = result.nodeResults().get(MGMT_KEY);
        assertNotNull(mgmt, "management-layer result must be recorded at " + MGMT_KEY);
        assertEquals(Outcomes.APPROVED, mgmt.outcome());

        // Manager leaf must be recorded.
        NodeResult manager = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(manager, "manager result must be recorded at " + MANAGER_KEY);
        assertEquals(Outcomes.APPROVED, manager.outcome());
    }

    /**
     * HR approved, but both manager AND CFO reject → management-layer REJECTED → outer ALL gate
     * REJECTED → workflow routes to rejected end node.
     */
    @Test
    @Timeout(30)
    void nestedAll_hrApproved_butManagementBothRejected_routesToRejected() throws Exception {
        WorkflowDefinition def = LeaveProcess.nestedApprovalFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "bob", "days", 5));

        GenericWorkflow workflow = newWorkflowStub("nested-rejected-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));

        // HR approves.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        // Both management approvers reject — the inner ANY gate sees no approval → REJECTED.
        workflow.signal(approvalSignal(MANAGER_KEY, "rejected"));
        workflow.signal(approvalSignal(CFO_KEY, "rejected"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("rejected", result.finalNodeId());

        // Outer taskGroup must have recorded REJECTED (because inner ANY is REJECTED).
        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "outer approvals result must be recorded");
        assertEquals(Outcomes.REJECTED, approvals.outcome());

        // Management-layer must be REJECTED.
        NodeResult mgmt = result.nodeResults().get(MGMT_KEY);
        assertNotNull(mgmt, "management-layer result must be recorded at " + MGMT_KEY);
        assertEquals(Outcomes.REJECTED, mgmt.outcome());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────────

    private GenericWorkflow newWorkflowStub(String prefix) {
        return client.newWorkflowStub(
                GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(prefix + UUID.randomUUID())
                        .build());
    }

    /**
     * Build an approval signal whose {@code taskId} payload routes to the specified child node.
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
