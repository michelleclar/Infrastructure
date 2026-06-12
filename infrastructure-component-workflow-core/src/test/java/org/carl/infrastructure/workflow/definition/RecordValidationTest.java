package org.carl.infrastructure.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Verifies that record canonical constructors reject null/blank required fields and that defensive
 * copies actually prevent external mutation.
 */
class RecordValidationTest {

    // ---------- NodeDefinition ----------

    @Test
    void nodeDefinitionRejectsNullId() {
        assertThrows(
                NullPointerException.class,
                () -> new NodeDefinition(null, "L", "serviceTask", null, null));
    }

    @Test
    void nodeDefinitionRejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeDefinition("  ", "L", "serviceTask", null, null));
    }

    @Test
    void nodeDefinitionRejectsNullType() {
        assertThrows(
                NullPointerException.class, () -> new NodeDefinition("n1", "L", null, null, null));
    }

    @Test
    void nodeDefinitionRejectsBlankType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NodeDefinition("n1", "L", "", null, null));
    }

    @Test
    void nodeDefinitionAllowsNullLabelTemplateAndConfig() {
        NodeDefinition n = new NodeDefinition("n1", null, "serviceTask", null, null);
        assertEquals("n1", n.id());
        assertNull(n.label());
        assertNull(n.templateId());
        assertNull(n.config());
    }

    // ---------- EdgeDefinition ----------

    @Test
    void edgeDefinitionRejectsNullFrom() {
        assertThrows(
                NullPointerException.class, () -> new EdgeDefinition(null, "b", null, null));
    }

    @Test
    void edgeDefinitionRejectsBlankFrom() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EdgeDefinition("", "b", null, null));
    }

    @Test
    void edgeDefinitionRejectsNullTo() {
        assertThrows(
                NullPointerException.class, () -> new EdgeDefinition("a", null, null, null));
    }

    @Test
    void edgeDefinitionRejectsBlankTo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EdgeDefinition("a", "  ", null, null));
    }

    @Test
    void edgeDefinitionAllowsNullOptionalFields() {
        EdgeDefinition e = new EdgeDefinition("a", "b", null, null);
        assertEquals("a", e.from());
        assertEquals("b", e.to());
        assertNull(e.event());
        assertNull(e.when());
    }

    // ---------- WorkflowDefinition ----------

    @Test
    void workflowDefinitionRejectsNullId() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkflowDefinition(null, "N", List.of(), List.of(), null));
    }

    @Test
    void workflowDefinitionRejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkflowDefinition("", "N", List.of(), List.of(), null));
    }

    @Test
    void workflowDefinitionRejectsNullName() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkflowDefinition("w", null, List.of(), List.of(), null));
    }

    @Test
    void workflowDefinitionRejectsBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkflowDefinition("w", " ", List.of(), List.of(), null));
    }

    @Test
    void workflowDefinitionRejectsNullNodes() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkflowDefinition("w", "N", null, List.of(), null));
    }

    @Test
    void workflowDefinitionRejectsNullEdges() {
        assertThrows(
                NullPointerException.class,
                () -> new WorkflowDefinition("w", "N", List.of(), null, null));
    }

    @Test
    void workflowDefinitionRejectsBlankStartNodeId() {
        // startNodeId is optional but, when present, must be non-blank.
        List<NodeDefinition> nodes =
                List.of(new NodeDefinition("n1", null, "serviceTask", null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkflowDefinition("w", "N", nodes, List.of(), "   "));
    }

    @Test
    void workflowDefinitionRejectsUnknownStartNodeId() {
        // startNodeId, when present, must reference an existing node.
        List<NodeDefinition> nodes =
                List.of(new NodeDefinition("n1", null, "serviceTask", null, null));
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new WorkflowDefinition("w", "N", nodes, List.of(), "ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }

    @Test
    void workflowDefinitionAcceptsKnownStartNodeId() {
        List<NodeDefinition> nodes =
                List.of(new NodeDefinition("n1", null, "serviceTask", null, null));
        WorkflowDefinition def = new WorkflowDefinition("w", "N", nodes, List.of(), "n1");
        assertEquals("n1", def.startNodeId());
    }

    @Test
    void workflowDefinitionOfFactoryLeavesStartNodeIdNull() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "N",
                        List.of(new NodeDefinition("n1", null, "serviceTask", null, null)),
                        List.of());
        assertNull(def.startNodeId());
    }

    @Test
    void workflowDefinitionListsAreDefensivelyCopied() {
        List<NodeDefinition> nodes = new ArrayList<>();
        nodes.add(new NodeDefinition("n1", null, "serviceTask", null, null));
        List<EdgeDefinition> edges = new ArrayList<>();
        edges.add(new EdgeDefinition("n1", "n2", null, null));

        WorkflowDefinition def = new WorkflowDefinition("w", "N", nodes, edges, null);

        // External mutation must not leak through.
        nodes.clear();
        edges.clear();
        assertEquals(1, def.nodes().size());
        assertEquals(1, def.edges().size());

        // The exposed lists must themselves be immutable.
        assertThrows(
                UnsupportedOperationException.class,
                () -> def.nodes().add(new NodeDefinition("x", null, "serviceTask", null, null)));
        assertThrows(
                UnsupportedOperationException.class,
                () -> def.edges().add(new EdgeDefinition("a", "b", null, null)));
    }

    // ---------- NodeResult ----------

    @Test
    void nodeResultRejectsNullStatus() {
        assertThrows(NullPointerException.class, () -> new NodeResult(null, "X", Map.of(), null));
    }

    @Test
    void nodeResultAcceptsAllOtherFieldsNull() {
        NodeResult r = new NodeResult(NodeStatus.COMPLETED, null, null, null);
        assertNotNull(r.payload());
        assertTrue(r.payload().isEmpty());
        assertNull(r.outcome());
        assertNull(r.message());
    }

    // ---------- WorkflowEvent ----------

    @Test
    void workflowEventRejectsNullName() {
        assertThrows(NullPointerException.class, () -> new WorkflowEvent(null, null));
    }

    @Test
    void workflowEventRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new WorkflowEvent("   ", null));
    }

    @Test
    void workflowEventAllowsNullPayload() {
        WorkflowEvent e = new WorkflowEvent("submit", null);
        assertEquals("submit", e.name());
        assertNull(e.payload());
    }
}
