package org.carl.infrastructure.workflow.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.spi.Outcomes;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class NodeResultTest {

    @Test
    void waitingFactoryProducesWaitingStatusWithNullOutcome() {
        NodeResult r = NodeResult.waiting();
        assertEquals(NodeStatus.WAITING, r.status());
        assertNull(r.outcome(), "waiting() must have null outcome");
        assertNotNull(r.payload());
        assertTrue(r.payload().isEmpty());
        assertNull(r.message());
    }

    @Test
    void completedFactoryWithoutPayload() {
        NodeResult r = NodeResult.completed(Outcomes.SUCCESS);
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals("SUCCESS", r.outcome());
        assertNotNull(r.payload());
        assertTrue(r.payload().isEmpty());
        assertNull(r.message());
    }

    @Test
    void completedFactoryWithPayloadCopiesAndExposesData() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", 123);
        payload.put("currency", "CNY");

        NodeResult r = NodeResult.completed(Outcomes.APPROVED, payload);

        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals("APPROVED", r.outcome());
        assertEquals(123, r.payload().get("amount"));
        assertEquals("CNY", r.payload().get("currency"));

        // Mutation of the source must not be reflected in the result.
        payload.put("amount", 999);
        assertEquals(123, r.payload().get("amount"));

        // The payload exposed to callers must be immutable.
        assertThrows(UnsupportedOperationException.class, () -> r.payload().put("x", "y"));
    }

    @Test
    void failedFactoryUsesFailedOutcomeAndCarriesMessage() {
        NodeResult r = NodeResult.failed("boom");
        assertEquals(NodeStatus.FAILED, r.status());
        assertEquals(Outcomes.FAILED, r.outcome(), "failed() outcome must be Outcomes.FAILED");
        assertEquals("FAILED", r.outcome());
        assertEquals("boom", r.message());
        assertNotNull(r.payload());
        assertTrue(r.payload().isEmpty());
    }

    @Test
    void cancelledFactoryUsesCancelledOutcome() {
        NodeResult r = NodeResult.cancelled();
        assertEquals(NodeStatus.CANCELLED, r.status());
        assertEquals(
                Outcomes.CANCELLED, r.outcome(), "cancelled() outcome must be Outcomes.CANCELLED");
        assertEquals("CANCELLED", r.outcome());
        assertNull(r.message());
        assertTrue(r.payload().isEmpty());
    }

    @Test
    void canonicalConstructorAcceptsNullPayloadAsEmptyMap() {
        NodeResult r = new NodeResult(NodeStatus.COMPLETED, "X", null, null);
        assertNotNull(r.payload());
        assertTrue(r.payload().isEmpty());
    }

    @Test
    void canonicalConstructorRejectsNullStatus() {
        assertThrows(NullPointerException.class, () -> new NodeResult(null, "X", Map.of(), null));
    }

    @Test
    void nodeResultIsJsonRoundtripable() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        NodeResult original = NodeResult.completed(Outcomes.APPROVED, Map.of("a", 1, "b", "two"));

        String json = mapper.writeValueAsString(original);
        NodeResult back = mapper.readValue(json, NodeResult.class);

        assertEquals(original.status(), back.status());
        assertEquals(original.outcome(), back.outcome());
        assertEquals(original.payload(), back.payload());
        assertEquals(original.message(), back.message());

        // Null fields must not appear.
        String waitingJson = mapper.writeValueAsString(NodeResult.waiting());
        assertTrue(waitingJson.contains("\"status\":\"WAITING\""));
        // outcome and message are null on waiting() — they must not be present.
        assertTrue(!waitingJson.contains("\"outcome\""), "outcome=null must be omitted");
        assertTrue(!waitingJson.contains("\"message\""), "message=null must be omitted");
    }

    @Test
    void emptyPayloadFactoriesShareTheSameImmutableInstance() {
        // Map.of() returns the singleton empty map; verify the contract is honoured by the
        // canonical constructor when payload happens to already be empty.
        NodeResult waiting1 = NodeResult.waiting();
        NodeResult waiting2 = NodeResult.waiting();
        assertSame(waiting1.payload(), waiting2.payload());
    }
}
