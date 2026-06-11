package org.carl.infrastructure.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Pure-Java unit tests for {@link EdgeRouter} — <strong>no Temporal runtime involved</strong>.
 * Proves the start-node / edge-routing decisions that used to live inside the Temporal adapter
 * ({@code GenericWorkflowImpl}) are now testable on their own.
 */
class EdgeRouterTest {

    private static NodeDefinition node(String id, String type) {
        return new NodeDefinition(id, null, type, null, null);
    }

    private static EdgeDefinition on(String from, String to, String event) {
        return new EdgeDefinition(from, to, event, null, null);
    }

    private static EdgeDefinition onWhen(String from, String to, String event, String when) {
        return new EdgeDefinition(from, to, event, null, when);
    }

    // ---- resolveStartNode ---------------------------------------------------

    @Test
    void resolveStartNode_uniqueZeroIncoming() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(on("A", "B", Outcomes.SUCCESS)));
        assertEquals("A", EdgeRouter.resolveStartNode(null, def, new WorkflowGraph(def)));
    }

    @Test
    void resolveStartNode_requestedWins() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(on("A", "B", Outcomes.SUCCESS)));
        assertEquals("B", EdgeRouter.resolveStartNode("B", def, new WorkflowGraph(def)));
    }

    @Test
    void resolveStartNode_explicitStartNodeId() {
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "w", "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(on("A", "B", Outcomes.SUCCESS)),
                        "A");
        assertEquals("A", EdgeRouter.resolveStartNode(null, def, new WorkflowGraph(def)));
    }

    @Test
    void resolveStartNode_ambiguousThrows() {
        // Two zero-incoming nodes (A and X) and no explicit start → unresolvable.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("X", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.END_TASK)),
                        List.of(on("A", "B", Outcomes.SUCCESS), on("X", "B", Outcomes.SUCCESS)));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertThrows(
                IllegalStateException.class, () -> EdgeRouter.resolveStartNode(null, def, graph));
    }

    // ---- pickNextEdge -------------------------------------------------------

    @Test
    void pickNextEdge_byEvent() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(
                                node("A", NodeTypes.APPROVAL_TASK),
                                node("B", NodeTypes.END_TASK),
                                node("C", NodeTypes.END_TASK)),
                        List.of(
                                on("A", "B", Outcomes.APPROVED),
                                on("A", "C", Outcomes.REJECTED)));
        WorkflowGraph graph = new WorkflowGraph(def);
        EdgeDefinition e =
                EdgeRouter.pickNextEdge(graph, "A", Outcomes.APPROVED, new StubCtx(Map.of()));
        assertNotNull(e);
        assertEquals("B", e.to());
    }

    @Test
    void pickNextEdge_noMatchReturnsNull() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(node("A", NodeTypes.APPROVAL_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(on("A", "B", Outcomes.APPROVED)));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertNull(EdgeRouter.pickNextEdge(graph, "A", Outcomes.REJECTED, new StubCtx(Map.of())));
    }

    @Test
    void pickNextEdge_guardedEdgeSelectedByCondition() {
        // Two edges share outcome SUCCESS: one guarded by ${large}, the other the fall-through.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("big", NodeTypes.END_TASK),
                                node("normal", NodeTypes.END_TASK)),
                        List.of(
                                onWhen("A", "big", Outcomes.SUCCESS, "${large}"),
                                on("A", "normal", Outcomes.SUCCESS)));
        WorkflowGraph graph = new WorkflowGraph(def);

        assertEquals(
                "big",
                EdgeRouter.pickNextEdge(graph, "A", Outcomes.SUCCESS, new StubCtx(Map.of("large", true)))
                        .to());
        assertEquals(
                "normal",
                EdgeRouter.pickNextEdge(graph, "A", Outcomes.SUCCESS, new StubCtx(Map.of("large", false)))
                        .to());
    }

    /** Minimal read-only context: only {@link #variables()} matters for guard evaluation. */
    private record StubCtx(Map<String, Object> vars) implements NodeExecutionContext {
        @Override
        public String workflowId() {
            return "wf";
        }

        @Override
        public String instanceId() {
            return "run";
        }

        @Override
        public String currentNodeId() {
            return "A";
        }

        @Override
        public JsonNode businessData() {
            return null;
        }

        @Override
        public Map<String, Object> variables() {
            return vars;
        }

        @Override
        public NodeResult resultOf(String nodeId) {
            return null;
        }

        @Override
        public WorkflowEvent currentEvent() {
            return null;
        }
    }
}
