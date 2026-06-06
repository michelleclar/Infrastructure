package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

class WorkflowEventJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundtripWithPayload() throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("approver", "carl");
        payload.put("amount", 1000);

        WorkflowEvent original = new WorkflowEvent("submit", payload);
        String json = mapper.writeValueAsString(original);
        WorkflowEvent back = mapper.readValue(json, WorkflowEvent.class);

        assertEquals(original, back);
        assertEquals("submit", back.name());
        assertNotNull(back.payload());
        assertEquals("carl", back.payload().path("approver").asText());
        assertEquals(1000, back.payload().path("amount").asInt());
    }

    @Test
    void roundtripWithoutPayload() throws Exception {
        WorkflowEvent original = new WorkflowEvent("submit", null);
        String json = mapper.writeValueAsString(original);

        assertFalse(json.contains("payload"), "payload=null must be omitted from JSON");

        WorkflowEvent back = mapper.readValue(json, WorkflowEvent.class);
        assertEquals("submit", back.name());
        assertNull(back.payload());
    }

    @Test
    void missingPayloadOnInputBecomesNull() throws Exception {
        WorkflowEvent e = mapper.readValue("{\"name\":\"submit\"}", WorkflowEvent.class);
        assertEquals("submit", e.name());
        assertNull(e.payload());
    }

    @Test
    void emptyObjectPayloadIsPreserved() throws Exception {
        WorkflowEvent e = mapper.readValue("{\"name\":\"x\",\"payload\":{}}", WorkflowEvent.class);
        JsonNode payload = e.payload();
        assertNotNull(payload, "explicit {} must not collapse to null");
        assertTrue(payload.isObject());
        assertEquals(0, payload.size());
    }
}
