package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

class TimerTaskHandlerTest {

    private final TimerTaskHandler handler = new TimerTaskHandler();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.TIMER_TASK, handler.type());
        assertEquals(TimerTaskConfig.class, handler.configType());
    }

    @Test
    void runEncodesDuration() {
        NodeResult r = handler.run(new TestContext(), new TimerTaskConfig("PT15M"));
        assertEquals(NodeStatus.WAITING, r.status());
        assertEquals("PT15M", r.payload().get(RuntimeIntents.DURATION));
    }

    @Test
    void canAcceptFiredOrCancel() {
        TimerTaskConfig cfg = new TimerTaskConfig("PT1S");
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_timerFired", null), cfg));
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_cancel", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), cfg));
    }

    @Test
    void onEventResolvesOutcome() {
        TimerTaskConfig cfg = new TimerTaskConfig("PT1S");
        TestContext ctx = new TestContext();
        assertEquals(
                "TRIGGERED",
                handler.onEvent(ctx, new WorkflowEvent("_timerFired", null), cfg).outcome());
        assertEquals(
                "CANCELLED",
                handler.onEvent(ctx, new WorkflowEvent("_cancel", null), cfg).outcome());
        assertEquals(
                NodeStatus.WAITING,
                handler.onEvent(ctx, new WorkflowEvent("other", null), cfg).status());
    }
}
