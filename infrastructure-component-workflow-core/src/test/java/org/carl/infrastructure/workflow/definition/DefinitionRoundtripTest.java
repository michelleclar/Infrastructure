package org.carl.infrastructure.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Verifies that the leave workflow example from {@code docs/workflow-v2-design.md} (the canonical
 * JSON shown in lines 375-443) deserialises into a {@link WorkflowDefinition}, then survives a JSON
 * round-trip while preserving structural equality.
 */
class DefinitionRoundtripTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void leaveWorkflowSurvivesRoundTrip() throws Exception {
        String original = loadResource("leave-workflow.json");

        WorkflowDefinition first = mapper.readValue(original, WorkflowDefinition.class);
        String serialized = mapper.writeValueAsString(first);
        WorkflowDefinition second = mapper.readValue(serialized, WorkflowDefinition.class);

        assertEquals(first, second, "round-trip must preserve structural equality");
        assertNotNull(serialized);

        // null fields must not appear in serialized output (templateId, outcome, when).
        assertFalse(serialized.contains("\"templateId\":null"), "templateId=null must be omitted");
        assertFalse(serialized.contains("\"outcome\":null"), "outcome=null must be omitted");
        assertFalse(serialized.contains("\"when\":null"), "when=null must be omitted");
    }

    @Test
    void leaveWorkflowStructureMatchesSpec() throws Exception {
        String original = loadResource("leave-workflow.json");
        WorkflowDefinition def = mapper.readValue(original, WorkflowDefinition.class);

        assertEquals("leave", def.id());
        assertEquals("请假流程", def.name());
        assertEquals(2, def.nodes().size());
        assertEquals(3, def.edges().size());

        NodeDefinition requestLeave = def.nodes().get(0);
        assertEquals("requestLeave", requestLeave.id());
        assertEquals("发起请假", requestLeave.label());
        assertEquals("serviceTask", requestLeave.type());
        assertNull(requestLeave.templateId());
        assertNotNull(requestLeave.config());
        assertEquals("createLeaveRequest", requestLeave.config().path("activity").asText());

        NodeDefinition leaveApproval = def.nodes().get(1);
        assertEquals("leaveApproval", leaveApproval.id());
        assertEquals("taskGroup", leaveApproval.type());

        // The nested tasks inside taskGroup config must remain a JsonNode subtree.
        JsonNode tasks = leaveApproval.config().path("tasks");
        assertTrue(tasks.isArray(), "tasks must be an array node");
        assertEquals(2, tasks.size());
        assertEquals("hrApproval", tasks.get(0).path("id").asText());
        assertEquals("managerApproval", tasks.get(1).path("id").asText());

        EdgeDefinition submitEdge = def.edges().get(0);
        assertEquals("requestLeave", submitEdge.from());
        assertEquals("leaveApproval", submitEdge.to());
        assertEquals("提交", submitEdge.event());
        assertNull(submitEdge.when());
    }

    @Test
    void missingOptionalFieldsRemainNull() throws Exception {
        String minimal =
                "{\"id\":\"w\",\"name\":\"W\",\"nodes\":[{\"id\":\"n1\",\"type\":\"serviceTask\"}],\"edges\":[]}";
        WorkflowDefinition def = mapper.readValue(minimal, WorkflowDefinition.class);
        NodeDefinition n1 = def.nodes().get(0);
        assertNull(n1.label());
        assertNull(n1.templateId());
        assertNull(n1.config());
    }

    @Test
    void emptyObjectConfigStaysAsObjectNode() throws Exception {
        String json =
                "{\"id\":\"w\",\"name\":\"W\",\"nodes\":[{\"id\":\"n1\",\"type\":\"serviceTask\",\"config\":{}}],\"edges\":[]}";
        WorkflowDefinition def = mapper.readValue(json, WorkflowDefinition.class);
        NodeDefinition n1 = def.nodes().get(0);
        assertNotNull(n1.config(), "explicit empty object must NOT collapse to null");
        assertTrue(n1.config().isObject());
        assertEquals(0, n1.config().size());

        // And it must round-trip back as {}, not be dropped.
        String back = mapper.writeValueAsString(def);
        assertTrue(back.contains("\"config\":{}"), "empty config object must be preserved");
    }

    private String loadResource(String name) throws Exception {
        try (InputStream in =
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(name),
                        "missing test resource: " + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
