package org.carl.infrastructure.workflow.runtime;

import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.any;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.dsl.JoinSpec;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.handlers.EndTaskHandler;
import org.carl.infrastructure.workflow.handlers.RuntimeIntents;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for {@link WorkflowInterpreter} driven by {@link FakeRuntimeOps} — the entire
 * orchestration loop (node execution, WAITING dispatch, routing, saga compensation) runs on a plain
 * JVM with <strong>no Temporal runtime</strong>. This is the payoff of stage 19a: editorial proof
 * that the engine's orchestration is Temporal-free.
 */
class WorkflowInterpreterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static WorkflowResult run(WorkflowDefinition def, FakeRuntimeOps ops) {
        WorkflowDefinition normalized = NodeConfigCodec.normalizeDefinition(def);
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(registry);
        WorkflowGraph graph = new WorkflowGraph(normalized);
        ExecutionContext ctx =
                new ExecutionContext("wf-1", "run-1", normalized.id(), null, Map.of());
        WorkflowInterpreter interp =
                new WorkflowInterpreter(
                        normalized,
                        graph,
                        registry,
                        MAPPER,
                        ctx,
                        ops,
                        new WorkflowInterceptorRegistry());
        return interp.run(null);
    }

    private static ObjectNode decision(String value) {
        return JsonNodeFactory.instance.objectNode().put("decision", value);
    }

    /** requestLeave (serviceTask) → approval → onLeave → completed / rejected / timedOut. */
    private static WorkflowDefinition approvalFlow() {
        FlowDef flow = Flow.define("interp-approval", "审批");
        flow.start("requestLeave");
        flow.node("requestLeave", BuiltInNodes.service("createLeave"));
        flow.node(
                "approval",
                BuiltInNodes.approval("mgr").andThen(b -> b.set("timeoutDuration", "PT24H")));
        flow.node("onLeave", BuiltInNodes.service("notify"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("拒绝"));
        flow.node("timedOut", b -> b.type(NodeTypes.END_TASK).label("超时"));
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("approval");
        flow.from("approval").on(Outcomes.APPROVED).to("onLeave");
        flow.from("approval").on(Outcomes.REJECTED).to("rejected");
        flow.from("approval").on(Outcomes.TIMEOUT).to("timedOut");
        flow.from("onLeave").on(Outcomes.SUCCESS).to("completed");
        return flow.build();
    }

    @Test
    void serviceTaskThenApprovalApproved_reachesCompleted() {
        FakeRuntimeOps ops = new FakeRuntimeOps().offerEvent(new WorkflowEvent("approval", decision("approved")));
        WorkflowResult result = run(approvalFlow(), ops);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());
        assertTrue(ops.activityCalls.contains("createLeave"), "serviceTask activity ran");
        assertTrue(ops.activityCalls.contains("notify"), "onLeave activity ran");
    }

    @Test
    void approvalRejected_reachesRejected() {
        FakeRuntimeOps ops = new FakeRuntimeOps().offerEvent(new WorkflowEvent("approval", decision("rejected")));
        WorkflowResult result = run(approvalFlow(), ops);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("rejected", result.finalNodeId());
    }

    @Test
    void approvalTimeout_reachesTimedOut() {
        // No event offered; the approval node has a timeout, so awaitEvent reports a timeout and the
        // handler maps it to the TIMEOUT outcome.
        FakeRuntimeOps ops = new FakeRuntimeOps();
        WorkflowResult result = run(approvalFlow(), ops);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("timedOut", result.finalNodeId());
    }

    @Test
    void failedNode_triggersSagaCompensation() {
        // svc1 (compensable, compensateActivity=undo1) succeeds → svc2 fails → compensation runs undo1.
        FlowDef flow = Flow.define("interp-saga", "补偿");
        flow.start("svc1");
        flow.node(
                "svc1",
                BuiltInNodes.service("act1").andThen(b -> b.set("compensateActivity", "undo1")));
        flow.node("svc2", BuiltInNodes.service("act2"));
        flow.node("done", b -> b.type(NodeTypes.END_TASK));
        flow.from("svc1").on(Outcomes.SUCCESS).to("svc2");
        flow.from("svc2").on(Outcomes.SUCCESS).to("done");

        FakeRuntimeOps ops =
                new FakeRuntimeOps()
                        .onActivity("act1", new ActivityResult(true, Map.of(), null))
                        .onActivity("act2", new ActivityResult(false, Map.of(), "boom"));

        WorkflowResult result = run(flow.build(), ops);

        assertEquals(NodeStatus.FAILED, result.finalStatus());
        assertEquals("svc2", result.finalNodeId());
        assertTrue(ops.activityCalls.contains("undo1"), "compensation activity undo1 ran");
    }

    @Test
    void conditionalRouting_picksGuardedEdge() {
        // requestLeave SUCCESS → two edges: ${large} → big, else → normal.
        FlowDef flow = Flow.define("interp-cond", "条件");
        flow.start("requestLeave");
        flow.node("requestLeave", BuiltInNodes.service("createLeave"));
        flow.node("big", b -> b.type(NodeTypes.END_TASK));
        flow.node("normal", b -> b.type(NodeTypes.END_TASK));
        flow.from("requestLeave").on(Outcomes.SUCCESS).when("${large}").to("big");
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("normal");

        // Drive the guard via an initial variable through businessData-free ctx: use variables.
        WorkflowDefinition def = NodeConfigCodec.normalizeDefinition(flow.build());
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(registry);
        WorkflowGraph graph = new WorkflowGraph(def);
        ExecutionContext ctx =
                new ExecutionContext("wf-1", "run-1", def.id(), null, Map.of("large", Boolean.TRUE));
        WorkflowInterpreter interp =
                new WorkflowInterpreter(
                        def, graph, registry, MAPPER, ctx, new FakeRuntimeOps(),
                        new WorkflowInterceptorRegistry());
        WorkflowResult result = interp.run(null);

        assertEquals("big", result.finalNodeId());
    }

    @Test
    void typedHandlerReceivesDecodedStateAndEventPayload() throws Exception {
        TypedWaitingHandler handler = new TypedWaitingHandler();
        NodeDefinition typedNode =
                new NodeDefinition(
                        "typed",
                        null,
                        TypedWaitingHandler.TYPE,
                        null,
                        MAPPER.readTree("{\"code\":\"A1\"}"));
        NodeDefinition endNode = new NodeDefinition("done", null, NodeTypes.END_TASK, null, null);
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "typed-flow",
                        "Typed Flow",
                        List.of(typedNode, endNode),
                        List.of(new EdgeDefinition("typed", "done", Outcomes.SUCCESS, null)),
                        "typed");

        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.registerBuiltIn(handler);
        registry.registerBuiltIn(new EndTaskHandler());
        ExecutionContext ctx =
                new ExecutionContext(
                        "wf-typed",
                        "run-typed",
                        def.id(),
                        MAPPER.readTree("{\"owner\":\"alice\",\"amount\":1200}"),
                        Map.of());
        FakeRuntimeOps ops =
                new FakeRuntimeOps()
                        .offerEvent(new WorkflowEvent("ignored", MAPPER.readTree("\"broken\"")))
                        .offerEvent(
                                new WorkflowEvent(
                                        "typedEvent",
                                        MAPPER.readTree("{\"decision\":\"approved\"}")));
        WorkflowInterpreter interp =
                new WorkflowInterpreter(
                        def,
                        new WorkflowGraph(def),
                        registry,
                        MAPPER,
                        ctx,
                        ops,
                        new WorkflowInterceptorRegistry());

        WorkflowResult result = interp.run(null);

        assertEquals("done", result.finalNodeId());
        assertEquals("A1", handler.runConfig.code());
        assertEquals("alice", handler.runState.owner());
        assertEquals(1200, handler.runState.amount());
        assertEquals("approved", handler.acceptedPayload.decision());
        assertEquals("approved", handler.deliveredPayload.decision());
    }

    @Test
    void typedCompensationReceivesDecodedState() throws Exception {
        CompletingCompensableHandler handler = new CompletingCompensableHandler();
        FailingHandler failingHandler = new FailingHandler();
        NodeDefinition okNode =
                new NodeDefinition(
                        "ok",
                        null,
                        CompletingCompensableHandler.TYPE,
                        null,
                        MAPPER.readTree("{\"code\":\"B2\"}"));
        NodeDefinition failNode = new NodeDefinition("fail", null, FailingHandler.TYPE, null, null);
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "typed-compensation",
                        "Typed Compensation",
                        List.of(okNode, failNode),
                        List.of(new EdgeDefinition("ok", "fail", Outcomes.SUCCESS, null)),
                        "ok");

        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.registerBuiltIn(handler);
        registry.registerBuiltIn(failingHandler);
        ExecutionContext ctx =
                new ExecutionContext(
                        "wf-comp",
                        "run-comp",
                        def.id(),
                        MAPPER.readTree("{\"owner\":\"alice\",\"amount\":1200}"),
                        Map.of());
        WorkflowInterpreter interp =
                new WorkflowInterpreter(
                        def,
                        new WorkflowGraph(def),
                        registry,
                        MAPPER,
                        ctx,
                        new FakeRuntimeOps(),
                        new WorkflowInterceptorRegistry());

        WorkflowResult result = interp.run(null);

        assertEquals(NodeStatus.FAILED, result.finalStatus());
        assertEquals("fail", result.finalNodeId());
        assertEquals("alice", handler.runState.owner());
        assertEquals("alice", handler.compensatedState.owner());
        assertEquals(1200, handler.compensatedState.amount());
    }

    // ── taskGroup join policy (the interpreter owns the loop; 19b) ────────────────────────────

    private static com.fasterxml.jackson.databind.node.ObjectNode decisionTask(
            String decision, String taskId) {
        return JsonNodeFactory.instance.objectNode().put("decision", decision).put("taskId", taskId);
    }

    /** requestLeave → approvals (taskGroup ALL/ANY of hr+mgr) → done / rejected. */
    private static WorkflowDefinition coSignFlow(boolean anyJoin) {
        FlowDef flow = Flow.define("interp-tg", "会签");
        flow.start("requestLeave");
        flow.node("requestLeave", BuiltInNodes.service("createLeave"));
        flow.node("done", b -> b.type(NodeTypes.END_TASK));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK));
        flow.from("requestLeave").on(Outcomes.SUCCESS).to("approvals");
        JoinSpec join =
                anyJoin
                        ? any(
                                node("hr", BuiltInNodes.approval("hr")),
                                node("mgr", BuiltInNodes.approval("mgr")))
                        : all(
                                node("hr", BuiltInNodes.approval("hr")),
                                node("mgr", BuiltInNodes.approval("mgr")));
        flow.from("approvals")
                .join(join)
                .on(Outcomes.APPROVED)
                .to("done")
                .on(Outcomes.REJECTED)
                .to("rejected");
        return flow.build();
    }

    @Test
    void taskGroupAll_bothApproved_reachesDone() {
        FakeRuntimeOps ops =
                new FakeRuntimeOps()
                        .offerEvent(new WorkflowEvent("approval", decisionTask("approved", "hr")))
                        .offerEvent(new WorkflowEvent("approval", decisionTask("approved", "mgr")));
        assertEquals("done", run(coSignFlow(false), ops).finalNodeId());
    }

    @Test
    void taskGroupAll_oneRejected_reachesRejected() {
        FakeRuntimeOps ops =
                new FakeRuntimeOps()
                        .offerEvent(new WorkflowEvent("approval", decisionTask("rejected", "hr")))
                        .offerEvent(new WorkflowEvent("approval", decisionTask("approved", "mgr")));
        assertEquals("rejected", run(coSignFlow(false), ops).finalNodeId());
    }

    @Test
    void taskGroupAny_firstApproved_reachesDone() {
        FakeRuntimeOps ops =
                new FakeRuntimeOps()
                        .offerEvent(new WorkflowEvent("approval", decisionTask("approved", "hr")))
                        .offerEvent(new WorkflowEvent("approval", decisionTask("approved", "mgr")));
        assertEquals("done", run(coSignFlow(true), ops).finalNodeId());
    }

    private record TypedConfig(String code) {}

    private record TypedState(String owner, int amount) {}

    private record TypedEvent(String decision) {}

    private static final class TypedWaitingHandler
            implements NodeHandler<TypedConfig, TypedState, TypedEvent> {

        static final String TYPE = "typedWaiting";

        TypedConfig runConfig;
        TypedState runState;
        TypedEvent acceptedPayload;
        TypedEvent deliveredPayload;

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Class<TypedConfig> configType() {
            return TypedConfig.class;
        }

        @Override
        public Class<TypedState> stateType() {
            return TypedState.class;
        }

        @Override
        public Class<TypedEvent> eventType() {
            return TypedEvent.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.SUCCESS);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, TypedConfig config) {
            throw new AssertionError("typed run overload was not used");
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, TypedConfig config, TypedState state) {
            this.runConfig = config;
            this.runState = state;
            return new NodeResult(
                    NodeStatus.WAITING,
                    null,
                    Map.of(RuntimeIntents.AWAIT_EVENT, "typedEvent"),
                    null);
        }

        @Override
        public boolean canAccept(
                NodeExecutionContext ctx, WorkflowEvent event, TypedConfig config) {
            return event != null && "typedEvent".equals(event.name());
        }

        @Override
        public boolean canAccept(
                NodeExecutionContext ctx,
                WorkflowEvent event,
                TypedConfig config,
                TypedEvent eventPayload) {
            this.acceptedPayload = eventPayload;
            return "approved".equals(eventPayload.decision());
        }

        @Override
        public NodeResult onEvent(
                NodeExecutionContext ctx,
                WorkflowEvent event,
                TypedConfig config,
                TypedEvent eventPayload) {
            this.deliveredPayload = eventPayload;
            return NodeResult.completed(Outcomes.SUCCESS);
        }
    }

    private static final class CompletingCompensableHandler
            implements NodeHandler<TypedConfig, TypedState, Object> {

        static final String TYPE = "typedCompensable";

        TypedState runState;
        TypedState compensatedState;

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Class<TypedConfig> configType() {
            return TypedConfig.class;
        }

        @Override
        public Class<TypedState> stateType() {
            return TypedState.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.SUCCESS);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, TypedConfig config) {
            throw new AssertionError("typed run overload was not used");
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, TypedConfig config, TypedState state) {
            this.runState = state;
            return NodeResult.completed(Outcomes.SUCCESS);
        }

        @Override
        public boolean compensable() {
            return true;
        }

        @Override
        public NodeResult compensate(
                NodeExecutionContext ctx,
                TypedConfig config,
                NodeResult completedResult,
                TypedState state) {
            this.compensatedState = state;
            return NodeResult.completed(Outcomes.SUCCESS);
        }
    }

    private static final class FailingHandler implements NodeHandler<Void, Object, Object> {

        static final String TYPE = "typedFailing";

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.FAILED);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            return NodeResult.failed("downstream failed");
        }
    }
}
