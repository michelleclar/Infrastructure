package org.carl.infrastructure.audit;

import org.carl.infrastructure.audit.core.AuditContext;
import org.carl.infrastructure.audit.model.AuditEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditContextTest {

    private AuditContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new AuditContext();
        AuditContext.clearCurrentOperator();
    }

    @AfterEach
    void tearDown() {
        AuditContext.clearCurrentOperator();
    }

    @Test
    void recordEvent_storesAndCanBeQueried() {
        AuditEvent event = new AuditEvent(1L, "Order", "O-1", "CREATE", "admin", "created", Instant.now());
        ctx.record(event);

        List<AuditEvent> results = ctx.query("Order", "O-1");
        assertEquals(1, results.size());
        assertEquals("CREATE", results.get(0).getAction());
        assertEquals("admin", results.get(0).getOperator());
    }

    @Test
    void recordConvenience_autoSetsTimestampAndOperator() {
        ctx.record("Order", "O-2", "UPDATE", "some details");

        List<AuditEvent> results = ctx.query("Order", "O-2");
        assertEquals(1, results.size());
        AuditEvent event = results.get(0);
        assertEquals("UPDATE", event.getAction());
        assertEquals("system", event.getOperator());
        assertNotNull(event.getTimestamp());
        assertEquals("some details", event.getDetails());
    }

    @Test
    void query_filtersByEntityTypeAndId() {
        ctx.record("Order", "O-1", "CREATE", "d1");
        ctx.record("Order", "O-2", "CREATE", "d2");
        ctx.record("Invoice", "O-1", "CREATE", "d3");

        List<AuditEvent> results = ctx.query("Order", "O-1");
        assertEquals(1, results.size());
        assertEquals("Order", results.get(0).getEntityType());
        assertEquals("O-1", results.get(0).getEntityId());
    }

    @Test
    void getCurrentOperator_defaultsToSystem() {
        assertEquals("system", AuditContext.getCurrentOperator());
    }

    @Test
    void recordConvenience_usesCustomOperatorFromThreadLocal() {
        AuditContext.setCurrentOperator("alice");
        try {
            ctx.record("Order", "O-3", "DELETE", "removed");

            List<AuditEvent> results = ctx.query("Order", "O-3");
            assertEquals(1, results.size());
            assertEquals("alice", results.get(0).getOperator());
        } finally {
            AuditContext.clearCurrentOperator();
        }
    }
}
