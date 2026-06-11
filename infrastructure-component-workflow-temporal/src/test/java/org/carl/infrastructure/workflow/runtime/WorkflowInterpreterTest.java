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

import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.dsl.JoinSpec;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
}
