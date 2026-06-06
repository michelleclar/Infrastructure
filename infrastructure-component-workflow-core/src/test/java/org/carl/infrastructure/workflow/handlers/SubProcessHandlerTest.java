package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class SubProcessHandlerTest {

    private final SubProcessHandler handler = new SubProcessHandler();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.SUB_PROCESS, handler.type());
        assertEquals(SubProcessConfig.class, handler.configType());
        assertEquals(
                Set.of(Outcomes.COMPLETED, Outcomes.CANCELLED, Outcomes.FAILED),
                handler.outcomes());
    }

    @Test
    void runEncodesSubWorkflowIntent() {
        SubProcessConfig cfg = new SubProcessConfig("payOrder", null, Map.of("orderId", 42), null);
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
        Map<String, Object> p = r.payload();
        assertEquals("payOrder", p.get(RuntimeIntents.SUB_WORKFLOW_ID));
        assertEquals(Map.of("orderId", 42), p.get(RuntimeIntents.SUB_INPUT));
    }

    @Test
    void canAcceptCompletionEvent() {
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_subProcessCompleted", null), null));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), null));
    }

    @Test
    void onEventPassesThroughOutcome() throws Exception {
        SubProcessConfig cfg = new SubProcessConfig("x", null, null, null);
        WorkflowEvent ev =
                new WorkflowEvent(
                        "_subProcessCompleted", mapper.readTree("{\"subOutcome\":\"COMPLETED\"}"));
        assertEquals(Outcomes.COMPLETED, handler.onEvent(new TestContext(), ev, cfg).outcome());
    }

    @Test
    void onEventAppliesOutcomeMapping() throws Exception {
        SubProcessConfig cfg =
                new SubProcessConfig("x", null, null, Map.of("MY_DONE", Outcomes.COMPLETED));
        WorkflowEvent ev =
                new WorkflowEvent(
                        "_subProcessCompleted", mapper.readTree("{\"subOutcome\":\"MY_DONE\"}"));
        assertEquals(Outcomes.COMPLETED, handler.onEvent(new TestContext(), ev, cfg).outcome());
    }

    @Test
    void onEventMissingSubOutcomeFails() {
        SubProcessConfig cfg = new SubProcessConfig("x", null, null, null);
        WorkflowEvent ev = new WorkflowEvent("_subProcessCompleted", null);
        NodeResult r = handler.onEvent(new TestContext(), ev, cfg);
        assertEquals(NodeStatus.FAILED, r.status());
    }
}
