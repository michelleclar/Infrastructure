package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class ServiceTaskHandlerTest {

    private final ServiceTaskHandler handler = new ServiceTaskHandler();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.SERVICE_TASK, handler.type());
        assertEquals(ServiceTaskConfig.class, handler.configType());
        assertEquals(Set.of(Outcomes.SUCCESS, Outcomes.FAILED), handler.outcomes());
        assertTrue(handler.compensable());
    }

    @Test
    void runReturnsWaitingWithActivityIntent() {
        ServiceTaskConfig cfg =
                new ServiceTaskConfig("createLeaveRequest", Map.of("days", 3), null);
        NodeResult result = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.WAITING, result.status());
        Map<String, Object> payload = result.payload();
        assertEquals("createLeaveRequest", payload.get(RuntimeIntents.ACTIVITY));
        assertEquals(Map.of("days", 3), payload.get(RuntimeIntents.ACTIVITY_INPUT));
    }

    @Test
    void canAcceptOnlyActivityResultEvent() {
        ServiceTaskConfig cfg = new ServiceTaskConfig("a", null, null);
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_activityResult", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), cfg));
        assertFalse(handler.canAccept(ctx, null, cfg));
    }

    @Test
    void onEventSuccessYieldsSuccessOutcome() throws Exception {
        ServiceTaskConfig cfg = new ServiceTaskConfig("a", null, null);
        WorkflowEvent event =
                new WorkflowEvent("_activityResult", mapper.readTree("{\"status\":\"success\"}"));
        NodeResult r = handler.onEvent(new TestContext(), event, cfg);
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals(Outcomes.SUCCESS, r.outcome());
    }

    @Test
    void onEventSuccessWithOutputPreservesOutputInPayload() throws Exception {
        ServiceTaskConfig cfg = new ServiceTaskConfig("a", null, null);
        WorkflowEvent event =
                new WorkflowEvent(
                        "_activityResult",
                        mapper.readTree(
                                "{\"status\":\"success\",\"output\":{\"orderId\":\"ORD-42\",\"total\":99.9}}"));
        NodeResult r = handler.onEvent(new TestContext(), event, cfg);
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals(Outcomes.SUCCESS, r.outcome());
        assertNotNull(r.payload().get("output"), "output must be present in NodeResult payload");
    }

    @Test
    void onEventSuccessWithNoOutputReturnsEmptyPayload() throws Exception {
        ServiceTaskConfig cfg = new ServiceTaskConfig("a", null, null);
        WorkflowEvent event =
                new WorkflowEvent("_activityResult", mapper.readTree("{\"status\":\"success\"}"));
        NodeResult r = handler.onEvent(new TestContext(), event, cfg);
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertTrue(r.payload().isEmpty(), "payload should be empty when no output field");
    }

    @Test
    void onEventFailureYieldsFailed() throws Exception {
        ServiceTaskConfig cfg = new ServiceTaskConfig("a", null, null);
        WorkflowEvent event =
                new WorkflowEvent(
                        "_activityResult",
                        mapper.readTree("{\"status\":\"failed\",\"message\":\"boom\"}"));
        NodeResult r = handler.onEvent(new TestContext(), event, cfg);
        assertEquals(NodeStatus.FAILED, r.status());
        assertEquals("boom", r.message());
    }

    @Test
    void onEventUnknownEventStaysWaiting() {
        NodeResult r = handler.onEvent(new TestContext(), new WorkflowEvent("nope", null), null);
        assertEquals(NodeStatus.WAITING, r.status());
    }

    @Test
    void runWithNullConfigStillReturnsWaiting() {
        NodeResult r = handler.run(new TestContext(), null);
        assertEquals(NodeStatus.WAITING, r.status());
        assertNotNull(r.payload());
    }
}
