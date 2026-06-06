package org.carl.infrastructure.workflow.example.leave;

import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;
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
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
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
 * End-to-end demo of the flow-first DSL ({@link Flow}/{@link FlowDef}) building a Chinese-named
 * co-sign leave workflow and running it on the Temporal in-memory test environment.
 *
 * <p>The flow being exercised:
 *
 * <pre>
 * 发起请假 --(SUCCESS)--> 发起请假审批 (taskGroup ALL: [HR 审批, 部门主管审批])
 *   发起请假审批 --APPROVED--> 休假 (end)
 *   发起请假审批 --REJECTED--> 拒绝 (end)
 * </pre>
 *
 * <p>The {@code 发起请假} service node runs the {@code createLeaveRequest} activity from {@link
 * LeaveActivities}. The two approval children are driven by {@code "approval"} signals addressed
 * with a {@code taskId} payload field (see {@link
 * org.carl.infrastructure.workflow.handlers.ApprovalTaskHandler#PAYLOAD_TASK_ID}).
 */
class LeaveFlowDslDemoTest {

    private static final String TASK_QUEUE = "LEAVE_V2_DSL_DEMO_TEST";

    // Node names used in both the flow definition and the assertions.
    private static final String N_REQUEST = "发起请假";
    private static final String N_APPROVAL = "发起请假审批";
    private static final String N_HR = "HR 审批";
    private static final String N_MANAGER = "部门主管审批";
    private static final String N_APPROVED = "休假";
    private static final String N_REJECTED = "拒绝";

    // TaskGroup child keys used for signal routing.
    // TaskGroupContract.childKey(parentId, childId) = parentId + "/" + childId.
    // FlowDef uses the child.name() directly as the child id.
    private static final String HR_KEY = TaskGroupContract.childKey(N_APPROVAL, N_HR);
    private static final String MANAGER_KEY = TaskGroupContract.childKey(N_APPROVAL, N_MANAGER);

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

    // ---- test methods -------------------------------------------------------

    /**
     * Happy path: both HR and manager approve — the taskGroup (ALL) resolves to APPROVED and the
     * workflow reaches the 休假 end node.
     */
    @Test
    @Timeout(30)
    void coSign_allApproved_runsToCompleted() throws Exception {
        WorkflowDefinition def = buildCoSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("dsl-demo-all-approved-");
        WorkflowClient.start(workflow::execute, input);

        // Let the workflow reach the taskGroup waiting state.
        testEnv.sleep(Duration.ofSeconds(1));

        // Both approvers signal approval.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "approved"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals(N_APPROVED, result.finalNodeId());

        // Verify taskGroup group result.
        NodeResult approvals = result.nodeResults().get(N_APPROVAL);
        assertNotNull(approvals, "taskGroup result '" + N_APPROVAL + "' should be recorded");
        assertEquals(Outcomes.APPROVED, approvals.outcome());

        // Verify individual child results.
        NodeResult hr = result.nodeResults().get(HR_KEY);
        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(hr, "HR child result should be recorded under " + HR_KEY);
        assertNotNull(mgr, "Manager child result should be recorded under " + MANAGER_KEY);
        assertEquals(Outcomes.APPROVED, hr.outcome());
        assertEquals(Outcomes.APPROVED, mgr.outcome());

        // Verify the service node completed successfully.
        NodeResult request = result.nodeResults().get(N_REQUEST);
        assertNotNull(request, "service node '" + N_REQUEST + "' result should be recorded");
        assertEquals(Outcomes.SUCCESS, request.outcome());
    }

    /**
     * Rejection path: manager rejects first — the taskGroup (ALL) short-circuits to REJECTED and
     * the workflow reaches the 拒绝 end node.
     */
    @Test
    @Timeout(30)
    void coSign_oneRejects_routesToRejected() throws Exception {
        WorkflowDefinition def = buildCoSignFlow();
        WorkflowInput input = newInput(def);

        GenericWorkflow workflow = newWorkflowStub("dsl-demo-one-rejected-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));

        // HR approves, manager rejects — ALL join → REJECTED.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "rejected"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals(N_REJECTED, result.finalNodeId());

        // Verify taskGroup group result.
        NodeResult approvals = result.nodeResults().get(N_APPROVAL);
        assertNotNull(approvals, "taskGroup result '" + N_APPROVAL + "' should be recorded");
        assertEquals(Outcomes.REJECTED, approvals.outcome());

        // The manager's rejection is recorded.
        NodeResult mgr = result.nodeResults().get(MANAGER_KEY);
        assertNotNull(mgr, "Manager child result should be recorded under " + MANAGER_KEY);
        assertEquals(Outcomes.REJECTED, mgr.outcome());
    }

    // ---- flow definition ----------------------------------------------------

    /**
     * Builds the co-sign leave flow using the flow-first DSL.
     *
     * <p>The workflow id / node ids are plain strings (Chinese names). The {@link FlowDef}
     * implementation uses them verbatim as {@link
     * org.carl.infrastructure.workflow.definition.NodeDefinition#id()}.
     */
    private static WorkflowDefinition buildCoSignFlow() {
        FlowDef flow = Flow.define("leaveCoSignDemo", "请假会签流程 Demo");

        flow.start(N_REQUEST);

        // Service node: create the leave request.
        flow.node(
                N_REQUEST,
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));

        // Route from the service node to the approval group on success.
        flow.from(N_REQUEST).on(Outcomes.SUCCESS).to(N_APPROVAL);

        // Declare the co-sign taskGroup via .join(all(...)).
        flow.from(N_APPROVAL)
                .join(
                        all(
                                node(N_HR, BuiltInNodes.approval("hr")),
                                node(N_MANAGER, BuiltInNodes.approval("manager"))))
                .on(Outcomes.APPROVED)
                .to(N_APPROVED)
                .on(Outcomes.REJECTED)
                .to(N_REJECTED);

        // End nodes.
        flow.node(N_APPROVED, BuiltInNodes.endTask());
        flow.node(N_REJECTED, BuiltInNodes.endTask());

        return flow.build();
    }

    // ---- helpers ------------------------------------------------------------

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

    /**
     * Builds an approval signal addressed to a specific taskGroup child via the {@code taskId}
     * payload field.
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
