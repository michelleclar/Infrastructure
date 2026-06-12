package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

class EventTaskHandlerTest {

    private final EventTaskHandler handler = new EventTaskHandler();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.EVENT_TASK, handler.type());
        assertEquals(EventTaskConfig.class, handler.configType());
    }

    @Test
    void runEncodesAwaitedEventAndTimeout() {
        EventTaskConfig cfg = new EventTaskConfig("paid", "PT10M");
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
        Map<String, Object> p = r.payload();
        assertEquals("paid", p.get(RuntimeIntents.AWAIT_EVENT));
        assertEquals("PT10M", p.get(RuntimeIntents.TIMEOUT_DURATION));
    }

    @Test
    void canAcceptAwaitedOrTimeout() {
        EventTaskConfig cfg = new EventTaskConfig("paid", null);
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("paid", null), cfg));
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_timeout", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), cfg));
        assertFalse(handler.canAccept(ctx, null, cfg));
    }

    @Test
    void onEventRoutesToOutcome() {
        EventTaskConfig cfg = new EventTaskConfig("paid", null);
        TestContext ctx = new TestContext();
        assertEquals(
                "RECEIVED",
                handler.onEvent(ctx, new WorkflowEvent("paid", null), cfg).outcome());
        assertEquals(
                "TIMEOUT",
                handler.onEvent(ctx, new WorkflowEvent("_timeout", null), cfg).outcome());
        assertEquals(
                NodeStatus.WAITING,
                handler.onEvent(ctx, new WorkflowEvent("other", null), cfg).status());
    }
}
