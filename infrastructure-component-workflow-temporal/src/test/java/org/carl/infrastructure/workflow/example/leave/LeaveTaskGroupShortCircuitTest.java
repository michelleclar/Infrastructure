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
 * End-to-end short-circuit tests for the {@code taskGroup} runtime.
 *
 * <p>These tests exercise the cancellation behaviour added to {@code
 * GenericWorkflowImpl.driveTaskGroup}: once the parent aggregator returns a non-WAITING outcome,
 * the surrounding {@link io.temporal.workflow.CancellationScope} is cancelled so any still-pending
 * child stops blocking the workflow immediately. We prove the bug is fixed by sending a signal to
 * only one of the two children — without short-circuiting the workflow would hang waiting for the
 * second signal and the {@code @Timeout(20)} on each test would fire.
 */
class LeaveTaskGroupShortCircuitTest {

    private static final String TASK_QUEUE = "LEAVE_V2_COSIGN_SHORTCIRCUIT_TEST";

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

    /**
     * ALL co-sign mode: as soon as the HR child reports REJECTED the parent aggregator decides
     * REJECTED and the still-pending manager child must be cancelled. We deliberately do NOT signal
     * {@code managerDecision}; if the runtime didn't short-circuit, the workflow would block until
     * {@link Timeout} fires.
     */
    @Test
    @Timeout(20)
    void allMode_oneRejectionShortCircuits_secondNotSignaled() throws Exception {
        WorkflowDefinition def = LeaveProcess.coSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-cosign-all-shortcircuit-");
        WorkflowClient.start(workflow::execute, input);

        // Let the workflow reach the taskGroup waiting state, then reject only the HR child.
        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(approvalSignal(HR_KEY, "rejected"));
        // Intentionally do NOT signal the manager child — short-circuit must unblock the workflow.

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(
                NodeStatus.COMPLETED,
                result.finalStatus(),
                "workflow should have terminated via short-circuit, not failed");
        assertEquals(
                "rejected",
                result.finalNodeId(),
                "ALL+rejection should route to the 'rejected' end node");

        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "approvals taskGroup result should be recorded");
        assertEquals(Outcomes.REJECTED, approvals.outcome());

        NodeResult hr = result.nodeResults().get(HR_KEY);
        assertNotNull(hr, "HR child result should be recorded under " + HR_KEY);
        assertEquals(Outcomes.REJECTED, hr.outcome());

        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        // Manager child was waiting for its signal when the scope was cancelled — its result
        // should reflect the cancellation rather than be missing (the runtime drains the
        // cancelled promises so callers can audit them).
        assertNotNull(
                mgr, "Manager child result should be recorded (as CANCELLED) under " + MANAGER_KEY);
        assertTrue(
                mgr.status() == NodeStatus.CANCELLED || Outcomes.CANCELLED.equals(mgr.outcome()),
                "Manager child should be CANCELLED after short-circuit, was: " + mgr);
    }

    /**
     * ANY or-sign mode: a single APPROVED resolves the group. The other child must be cancelled
     * rather than left blocking on its signal.
     */
    @Test
    @Timeout(20)
    void anyMode_oneApprovalShortCircuits_secondNotSignaled() throws Exception {
        WorkflowDefinition def = LeaveProcess.coSignAnyFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("test-cosign-any-shortcircuit-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        // Intentionally do NOT signal the manager child.

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(
                NodeStatus.COMPLETED,
                result.finalStatus(),
                "workflow should have terminated via short-circuit, not failed");
        assertEquals(
                "completed",
                result.finalNodeId(),
                "ANY+single approval should route through the approved branch to 'completed'");

        NodeResult approvals = result.nodeResults().get("approvals");
        assertNotNull(approvals, "approvals taskGroup result should be recorded");
        assertEquals(Outcomes.APPROVED, approvals.outcome());

        NodeResult hr = result.nodeResults().get(HR_KEY);
        assertNotNull(hr, "HR child result should be recorded under " + HR_KEY);
        assertEquals(Outcomes.APPROVED, hr.outcome());

        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(
                mgr, "Manager child result should be recorded (as CANCELLED) under " + MANAGER_KEY);
        assertTrue(
                mgr.status() == NodeStatus.CANCELLED || Outcomes.CANCELLED.equals(mgr.outcome()),
                "Manager child should be CANCELLED after short-circuit, was: " + mgr);
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

    /** Unified "approval" signal addressed to a specific taskGroup child via {@code taskId}. */
    private static WorkflowEvent approvalSignal(String taskId, String decision) {
        ObjectNode payload =
                JsonNodeFactory.instance
                        .objectNode()
                        .put("taskId", taskId)
                        .put("decision", decision);
        return new WorkflowEvent("approval", payload);
    }
}
