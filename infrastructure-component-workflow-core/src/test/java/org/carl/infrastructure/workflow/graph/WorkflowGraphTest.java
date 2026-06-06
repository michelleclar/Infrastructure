package org.carl.infrastructure.workflow.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class WorkflowGraphTest {

    private static NodeDefinition node(String id, String type) {
        return new NodeDefinition(id, null, type, null, null);
    }

    private static EdgeDefinition event(String from, String to, String event) {
        return new EdgeDefinition(from, to, event, null, null);
    }

    private static EdgeDefinition outcome(String from, String to, String outcome) {
        return new EdgeDefinition(from, to, null, outcome, null);
    }

    @Test
    void simpleTwoNodeFlowSupportsByEventRouting() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(event("A", "B", "submit")));
        WorkflowGraph graph = new WorkflowGraph(def);

        assertEquals(Set.of("A", "B"), graph.nodeIds());
        assertEquals(1, graph.outgoing("A").size());
        assertEquals("submit", graph.outgoing("A").get(0).event());
        assertEquals(0, graph.outgoing("B").size());
        assertEquals(0, graph.incoming("A").size());
        assertEquals(1, graph.incoming("B").size());

        List<EdgeDefinition> picked = graph.nextCandidates("A", EdgeMatch.byEvent("submit"));
        assertEquals(1, picked.size());
        assertEquals("B", picked.get(0).to());

        List<EdgeDefinition> none = graph.nextCandidates("A", EdgeMatch.byEvent("nope"));
        assertTrue(none.isEmpty());

        assertTrue(graph.canAccept("A", new WorkflowEvent("submit", null)));
        assertFalse(graph.canAccept("A", new WorkflowEvent("other", null)));
    }

    @Test
    void taskGroupFlowSupportsByOutcomeRouting() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "leave",
                        "Leave",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("tg", NodeTypes.TASK_GROUP),
                                node("end", NodeTypes.END_TASK)),
                        List.of(
                                event("A", "tg", "submit"),
                                outcome("tg", "end", Outcomes.APPROVED),
                                outcome("tg", "A", Outcomes.REJECTED)));
        WorkflowGraph graph = new WorkflowGraph(def);

        List<EdgeDefinition> approved =
                graph.nextCandidates("tg", EdgeMatch.byOutcome(Outcomes.APPROVED));
        assertEquals(1, approved.size());
        assertEquals("end", approved.get(0).to());

        List<EdgeDefinition> rejected =
                graph.nextCandidates("tg", EdgeMatch.byOutcome(Outcomes.REJECTED));
        assertEquals(1, rejected.size());
        assertEquals("A", rejected.get(0).to());

        // EdgeMatch.any() returns every outgoing edge in original order
        List<EdgeDefinition> all = graph.nextCandidates("tg", EdgeMatch.any());
        assertEquals(2, all.size());
        assertEquals("end", all.get(0).to());
        assertEquals("A", all.get(1).to());
    }

    @Test
    void detectCyclesFindsTwoNodeLoop() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK)),
                        List.of(event("A", "B", "x"), event("B", "A", "y")));
        WorkflowGraph graph = new WorkflowGraph(def);

        List<List<String>> cycles = graph.detectCycles();
        assertEquals(1, cycles.size(), "expected exactly one SCC of size 2");
        assertEquals(2, cycles.get(0).size());
        assertTrue(cycles.get(0).contains("A"));
        assertTrue(cycles.get(0).contains("B"));
    }

    @Test
    void detectCyclesFindsSelfLoop() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("T", NodeTypes.TIMER_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("T", "T", "tick"), event("T", "end", "done")));
        WorkflowGraph graph = new WorkflowGraph(def);

        List<List<String>> cycles = graph.detectCycles();
        assertEquals(1, cycles.size());
        assertEquals(List.of("T"), cycles.get(0));
    }

    @Test
    void detectCyclesReturnsEmptyForAcyclic() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(event("A", "B", "submit")));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertTrue(graph.detectCycles().isEmpty());
    }

    @Test
    void canReachAndReachableFromCoverClosure() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("mid", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK),
                                node("orphan", NodeTypes.SERVICE_TASK)),
                        List.of(event("start", "mid", "x"), event("mid", "end", "y")));
        WorkflowGraph graph = new WorkflowGraph(def);

        assertTrue(graph.canReach("start", "end"));
        assertTrue(graph.canReach("start", "mid"));
        assertTrue(graph.canReach("start", "start"), "node reaches itself trivially");
        assertFalse(graph.canReach("start", "orphan"));
        assertFalse(graph.canReach("end", "start"));

        Set<String> closure = graph.reachableFrom("start");
        assertEquals(Set.of("start", "mid", "end"), closure);
        assertEquals(Set.of("orphan"), graph.reachableFrom("orphan"));
    }

    @Test
    void findNodeReturnsOptionalNodeThrowsOnMissing() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W", List.of(node("A", NodeTypes.SERVICE_TASK)), List.of());
        WorkflowGraph graph = new WorkflowGraph(def);

        Optional<NodeDefinition> found = graph.findNode("A");
        assertTrue(found.isPresent());
        assertEquals("A", found.get().id());

        assertTrue(graph.findNode("missing").isEmpty());
        assertTrue(graph.findNode(null).isEmpty());

        assertEquals("A", graph.node("A").id());
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> graph.node("missing"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    void outgoingAndIncomingRejectUnknownNode() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W", List.of(node("A", NodeTypes.SERVICE_TASK)), List.of());
        WorkflowGraph graph = new WorkflowGraph(def);

        assertThrows(IllegalArgumentException.class, () -> graph.outgoing("missing"));
        assertThrows(IllegalArgumentException.class, () -> graph.incoming("missing"));
        assertThrows(IllegalArgumentException.class, () -> graph.reachableFrom("missing"));
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.nextCandidates("missing", EdgeMatch.any()));
    }

    @Test
    void outgoingPreservesEdgeInsertionOrder() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK),
                                node("C", NodeTypes.SERVICE_TASK)),
                        List.of(
                                event("A", "B", "first"),
                                event("A", "C", "second"),
                                event("A", "B", "third")));
        WorkflowGraph graph = new WorkflowGraph(def);

        List<EdgeDefinition> out = graph.outgoing("A");
        assertEquals(3, out.size());
        assertEquals("first", out.get(0).event());
        assertEquals("second", out.get(1).event());
        assertEquals("third", out.get(2).event());
    }

    @Test
    void startAndEndNodesAreDerivedFromTopology() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("mid", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("start", "mid", "x"), event("mid", "end", "y")));
        WorkflowGraph graph = new WorkflowGraph(def);

        assertEquals(Set.of("start"), graph.startNodes());
        assertEquals(Set.of("end"), graph.endNodes());
    }

    @Test
    void endNodeRecognisedByTypeEvenWithOutgoing() {
        // edge case: an endTask wired to itself is still considered an end node by type.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK),
                                node("after", NodeTypes.SERVICE_TASK)),
                        List.of(event("start", "end", "go"), event("end", "after", "weird")));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertTrue(graph.endNodes().contains("end"));
        assertTrue(graph.endNodes().contains("after"));
    }

    @Test
    void definitionAccessorReturnsOriginal() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W", List.of(node("A", NodeTypes.SERVICE_TASK)), List.of());
        WorkflowGraph graph = new WorkflowGraph(def);
        assertNotNull(graph.definition());
        assertEquals("w", graph.definition().id());
    }

    @Test
    void effectiveStartNodePrefersExplicitOverTopology() {
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("mid", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("start", "mid", "x"), event("mid", "end", "y")),
                        "mid");
        WorkflowGraph graph = new WorkflowGraph(def);
        assertEquals("mid", graph.effectiveStartNode());
    }

    @Test
    void effectiveStartNodeFallsBackToTopology() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("start", "end", "go")));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertEquals("start", graph.effectiveStartNode());
    }

    @Test
    void effectiveStartNodeThrowsOnAmbiguousTopology() {
        // Two zero-incoming nodes, no explicit start -> must throw.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "go"), event("B", "end", "go")));
        WorkflowGraph graph = new WorkflowGraph(def);
        assertThrows(IllegalStateException.class, graph::effectiveStartNode);
    }

    @Test
    void backEdgeToExplicitStartIsLegal() {
        // The case the runtime needs: requestLeave is the explicit start; approval --REJECTED-->
        // requestLeave forms a back-edge. WorkflowGraph itself just indexes; the validator handles
        // the legality. Here we verify the back-edge appears in incoming and outgoing.
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "w",
                        "W",
                        List.of(
                                node("requestLeave", NodeTypes.SERVICE_TASK),
                                node("approval", NodeTypes.SERVICE_TASK),
                                node("done", NodeTypes.END_TASK)),
                        List.of(
                                event("requestLeave", "approval", "submit"),
                                outcome("approval", "done", Outcomes.APPROVED),
                                outcome("approval", "requestLeave", Outcomes.REJECTED)),
                        "requestLeave");
        WorkflowGraph graph = new WorkflowGraph(def);
        assertEquals("requestLeave", graph.effectiveStartNode());
        assertEquals(1, graph.incoming("requestLeave").size());
        assertEquals(2, graph.outgoing("approval").size());
    }
}
