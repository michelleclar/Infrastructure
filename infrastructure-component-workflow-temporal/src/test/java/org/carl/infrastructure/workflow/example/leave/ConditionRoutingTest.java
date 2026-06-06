package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end tests proving that {@link
 * org.carl.infrastructure.workflow.definition.EdgeDefinition#when()} EL guard expressions are
 * evaluated by the runtime's edge router. Two edges share the same outcome ({@code SUCCESS}); one
 * has a {@code ${largeAmount}} guard, the other has no guard and acts as the fall-through default.
 */
class ConditionRoutingTest {

    private static final String TASK_QUEUE = "LEAVE_V2_CONDITION_TEST";

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
    void largeAmountTrue_routesToBigApproval() throws Exception {
        WorkflowDefinition def = conditionalRoutingFlow();

        Map<String, Object> initialVariables = new LinkedHashMap<>();
        initialVariables.put("largeAmount", Boolean.TRUE);
        WorkflowInput input = newInput(def, initialVariables);

        GenericWorkflow workflow = newWorkflowStub("test-cond-big-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("bigApprovalDecision", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("done", result.finalNodeId());

        // The conditional edge fired: the big-approval node executed, the normal-approval node
        // did not.
        NodeResult big = result.nodeResults().get("bigApproval");
        assertNotNull(big, "bigApproval should have executed");
        assertEquals(Outcomes.APPROVED, big.outcome());
        assertNull(
                result.nodeResults().get("normalApproval"),
                "normalApproval must NOT have executed when largeAmount=true");
    }

    @Test
    @Timeout(30)
    void largeAmountFalse_routesToNormalApprovalViaFallthrough() throws Exception {
        WorkflowDefinition def = conditionalRoutingFlow();

        Map<String, Object> initialVariables = new LinkedHashMap<>();
        initialVariables.put("largeAmount", Boolean.FALSE);
        WorkflowInput input = newInput(def, initialVariables);

        GenericWorkflow workflow = newWorkflowStub("test-cond-normal-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));
        workflow.signal(new WorkflowEvent("normalApprovalDecision", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("done", result.finalNodeId());

        NodeResult normal = result.nodeResults().get("normalApproval");
        assertNotNull(normal, "normalApproval should have executed");
        assertEquals(Outcomes.APPROVED, normal.outcome());
        assertNull(
                result.nodeResults().get("bigApproval"),
                "bigApproval must NOT have executed when largeAmount=false");
    }

    /**
     * Two edges from {@code requestLeave} both have outcome {@code SUCCESS}; the first carries a
     * {@code ${largeAmount}} condition, the second is unconditional and acts as the default
     * fall-through.
     */
    private static WorkflowDefinition conditionalRoutingFlow() {
        FlowDef flow = Flow.define("condRouting", "条件路由 V2");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node(
                "bigApproval",
                BuiltInNodes.approval("director")
                        .andThen(b -> b.set("awaitEvent", "bigApprovalDecision")));
        flow.node(
                "normalApproval",
                BuiltInNodes.approval("manager")
                        .andThen(b -> b.set("awaitEvent", "normalApprovalDecision")));
        flow.node("done", b -> b.type(NodeTypes.END_TASK).label("已完成"));

        // Conditional edge: only taken when ${largeAmount} is truthy.
        flow.from("requestLeave").on(Outcomes.SUCCESS).when("${largeAmount}").to("bigApproval");
        // Default edge: same outcome but no condition; routed to whenever the conditional
        // edge above does not match.
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("normalApproval");
        flow.from("bigApproval").on(Outcomes.APPROVED).to("done");
        flow.from("normalApproval").on(Outcomes.APPROVED).to("done");

        return flow.build();
    }

    private WorkflowInput newInput(WorkflowDefinition def, Map<String, Object> initialVariables)
            throws Exception {
        return WorkflowInput.from(
                def, Map.of("employee", "alice", "days", 3), initialVariables, null);
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
