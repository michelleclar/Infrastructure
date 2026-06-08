package org.carl.infrastructure.workflow.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.InterceptorHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Integration tests verifying that {@link WorkflowInterceptorRegistry} interceptors are invoked
 * correctly by {@link org.carl.infrastructure.workflow.runtime.GenericWorkflowImpl}.
 */
class InterceptorIntegrationTest {

    private static final String TASK_QUEUE = "INTERCEPTOR_INTEGRATION_TEST";

    // Shared lists — populated by recording interceptors, cleared before each test.
    static final List<String> deterministicHits = new CopyOnWriteArrayList<>();
    static final List<String> asyncHits = new CopyOnWriteArrayList<>();

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private ObjectMapper mapper;

    // ── Recording interceptors ───────────────────────────────────────────────────────────────────

    static class RecordingDeterministic implements DeterministicInterceptor {

        @Override
        public int order() {
            return 1;
        }

        @Override
        public void onWorkflowStart(InterceptorContext ctx) {
            deterministicHits.add("start:" + ctx.definitionId());
        }

        @Override
        public void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {
            deterministicHits.add("enter:" + node.id());
        }

        @Override
        public void onNodeExit(InterceptorContext ctx, NodeDefinition node, NodeResult result) {
            deterministicHits.add("exit:" + node.id() + ":" + result.outcome());
        }

        @Override
        public void onWorkflowEnd(InterceptorContext ctx, NodeResult terminal) {
            deterministicHits.add("end:" + terminal.outcome());
        }

        @Override
        public void onNodeError(InterceptorContext ctx, NodeDefinition node, String errorMessage) {
            deterministicHits.add("error:" + node.id());
        }

        @Override
        public void onEvent(InterceptorContext ctx, WorkflowEvent event) {
            deterministicHits.add("event:" + event.name());
        }

        @Override
        public void onCompensate(
                InterceptorContext ctx, NodeDefinition node, NodeResult originalResult) {
            deterministicHits.add("compensate:" + node.id());
        }
    }

    static class ThrowingDeterministic implements DeterministicInterceptor {

        @Override
        public int order() {
            return 1;
        }

        @Override
        public void onNodeEnter(InterceptorContext ctx, NodeDefinition node) {
            throw new RuntimeException("deliberate interceptor failure on enter:" + node.id());
        }
    }

    // ── Test lifecycle ───────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        deterministicHits.clear();
        asyncHits.clear();

        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("doWork", input -> Map.of("result", "done"));

        WorkflowInterceptorRegistry interceptorRegistry = new WorkflowInterceptorRegistry();
        interceptorRegistry.register(new RecordingDeterministic());

        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, interceptorRegistry);

        testEnv.start();
        client = testEnv.getWorkflowClient();
        mapper = ObjectMapperHolder.mapper();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
        // Reset the InterceptorHolder to an empty registry so subsequent tests are not affected.
        InterceptorHolder.install(new WorkflowInterceptorRegistry());
    }

    // ── Test cases ───────────────────────────────────────────────────────────────────────────────

    /**
     * A 2-node workflow (serviceTask → endTask) must fire:
     * start, enter:work, exit:work, enter:completed, exit:completed, end
     * in exactly that order.
     */
    @Test
    @Timeout(30)
    void interceptor_seesEveryNode_inOrder() throws Exception {
        WorkflowDefinition def = twoNodeFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("key", "value"));

        WorkflowResult result = runSync(input);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());

        // Verify the deterministic interceptor saw the expected lifecycle events.
        // Expected: start, enter:work, event:_activityResult, exit:work,
        //           enter:completed, exit:completed, end
        assertTrue(
                deterministicHits.contains("start:interceptor-test-flow"),
                "WORKFLOW_START hook must fire; hits=" + deterministicHits);
        assertTrue(
                deterministicHits.contains("enter:work"),
                "NODE_ENTER work must fire; hits=" + deterministicHits);
        assertTrue(
                deterministicHits.contains("event:_activityResult"),
                "EVENT hook must fire for activity result; hits=" + deterministicHits);
        assertTrue(
                deterministicHits.contains("exit:work:SUCCESS"),
                "NODE_EXIT work must fire; hits=" + deterministicHits);
        assertTrue(
                deterministicHits.contains("enter:completed"),
                "NODE_ENTER completed must fire; hits=" + deterministicHits);
        assertTrue(
                deterministicHits.contains("end:COMPLETED"),
                "WORKFLOW_END hook must fire; hits=" + deterministicHits);
        // Total: start + enter:work + event:_activityResult + exit:work
        //        + enter:completed + exit:completed + end = 7
        assertEquals(7, deterministicHits.size(), "Expected exactly 7 hook hits; got: " + deterministicHits);
    }

    /**
     * An interceptor that throws on {@code onNodeEnter} must not kill the workflow.
     * The workflow must complete normally even when the interceptor fails.
     */
    @Test
    @Timeout(30)
    void interceptorErrors_doNotKillWorkflow() throws Exception {
        // Reset to use a throwing interceptor only.
        testEnv.close();
        deterministicHits.clear();

        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("doWork", input -> Map.of("result", "done"));

        WorkflowInterceptorRegistry throwingRegistry = new WorkflowInterceptorRegistry();
        throwingRegistry.register(new ThrowingDeterministic());

        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, throwingRegistry);
        testEnv.start();
        client = testEnv.getWorkflowClient();

        WorkflowDefinition def = twoNodeFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("key", "value"));

        WorkflowResult result = runSync(input);

        // Workflow must complete regardless of interceptor exceptions.
        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
    }

    /**
     * With no interceptors registered, the workflow must run identically to the pre-interceptor
     * behaviour and produce a correct result.
     */
    @Test
    @Timeout(30)
    void emptyRegistry_doesNotImpactWorkflow() throws Exception {
        // Reset to an empty registry.
        testEnv.close();
        deterministicHits.clear();

        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("doWork", input -> Map.of("result", "done"));

        // Explicitly install an empty registry so the static holder is clear.
        InterceptorHolder.install(new WorkflowInterceptorRegistry());
        // Use the simple 3-arg setup (no interceptors).
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);
        testEnv.start();
        client = testEnv.getWorkflowClient();

        WorkflowDefinition def = twoNodeFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("key", "value"));

        WorkflowResult result = runSync(input);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertTrue(deterministicHits.isEmpty(), "No interceptor hits expected");
    }

    /**
     * When a signal is accepted by handler.onEvent, the EVENT hook must fire with the event name.
     * Flow: approvalTask waits for "approval" signal → approved → endTask.
     */
    @Test
    @Timeout(30)
    void eventHook_firedOnSignalConsumption() throws Exception {
        WorkflowDefinition def = approvalFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "alice"));

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("event-hook-test-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);

        // Give the workflow time to reach the approvalTask await state, then signal approval.
        testEnv.sleep(Duration.ofSeconds(1));
        ObjectNode decisionPayload = JsonNodeFactory.instance.objectNode().put("decision", "approved");
        workflow.signal(new WorkflowEvent("approval", decisionPayload));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus(), "Workflow must complete after approval");
        assertTrue(
                deterministicHits.contains("event:approval"),
                "EVENT hook must fire with 'approval'; hits=" + deterministicHits);
    }

    /**
     * When a saga-compensable node rolls back, the COMPENSATE hook must fire before compensate().
     * Flow: node1 (compensable serviceTask) → node2 (serviceTask that fails) → compensation of node1.
     */
    @Test
    @Timeout(30)
    void compensateHook_firedOnSagaRollback() throws Exception {
        // Build a minimal saga: node1 (compensable) → node2 (fails).
        FlowDef flow = Flow.define("compensate-hook-test", "Compensate Hook Test");
        flow.start("node1");
        flow.node(
                "node1",
                BuiltInNodes.service("doCompensableWork")
                        .andThen(b -> b.set("compensateActivity", "undoWork")));
        flow.node("node2", BuiltInNodes.service("doFailingWork"));
        flow.from("node1").on(Outcomes.SUCCESS).to("node2");
        WorkflowDefinition def = flow.build();

        // Register the activities: doCompensableWork succeeds, doFailingWork throws,
        // undoWork (compensate) succeeds.
        testEnv.close();
        deterministicHits.clear();

        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("doCompensableWork", input -> Map.of("status", "done"));
        activityRegistry.register(
                "doFailingWork",
                input -> {
                    throw new RuntimeException("node2 always fails");
                });
        activityRegistry.register("undoWork", input -> Map.of());

        WorkflowInterceptorRegistry interceptorRegistry = new WorkflowInterceptorRegistry();
        interceptorRegistry.register(new RecordingDeterministic());

        WorkerSetup.setup(worker, handlerRegistry, activityRegistry, interceptorRegistry);
        testEnv.start();
        client = testEnv.getWorkflowClient();

        WorkflowInput input = WorkflowInput.from(def, null);
        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("compensate-hook-test-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);
        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.FAILED, result.finalStatus(), "Workflow must fail when node2 fails");
        assertTrue(
                deterministicHits.contains("compensate:node1"),
                "COMPENSATE hook must fire for node1; hits=" + deterministicHits);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────

    /** A simple 2-node workflow: serviceTask(work) → endTask(completed). */
    private static WorkflowDefinition twoNodeFlow() {
        FlowDef flow = Flow.define("interceptor-test-flow", "Interceptor Integration Test");
        flow.start("work");
        flow.node("work", BuiltInNodes.service("doWork"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("Completed"));
        flow.from("work").on(Outcomes.SUCCESS).to("completed");
        return flow.build();
    }

    /**
     * Minimal approval flow: approvalTask(approval) → endTask(approved) or endTask(rejected).
     * Used by eventHook_firedOnSignalConsumption.
     */
    private static WorkflowDefinition approvalFlow() {
        FlowDef flow = Flow.define("interceptor-approval-flow", "Approval Event Hook Test");
        flow.start("approval");
        flow.node(
                "approval",
                BuiltInNodes.approval("manager").andThen(b -> b.set("awaitEvent", "approval")));
        flow.node("approved", b -> b.type(NodeTypes.END_TASK).label("Approved"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("Rejected"));
        flow.from("approval").on(Outcomes.APPROVED).to("approved");
        flow.from("approval").on(Outcomes.REJECTED).to("rejected");
        return flow.build();
    }

    private WorkflowResult runSync(WorkflowInput input) {
        GenericWorkflow stub =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("interceptor-test-" + UUID.randomUUID())
                                .build());
        return stub.execute(input);
    }
}
