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

class ApprovalTaskHandlerTest {

    private final ApprovalTaskHandler handler = new ApprovalTaskHandler();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.APPROVAL_TASK, handler.type());
        assertEquals(ApprovalTaskConfig.class, handler.configType());
        assertEquals(
                Set.of(Outcomes.APPROVED, Outcomes.REJECTED, Outcomes.SENDBACK, Outcomes.TIMEOUT),
                handler.outcomes());
    }

    @Test
    void runEmitsAssigneeAwaitEventAndTimeoutInPayload() {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("user-1", "managerApproval", "PT24H");
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
        Map<String, Object> payload = r.payload();
        assertEquals("user-1", payload.get(RuntimeIntents.ASSIGNEE));
        assertEquals("managerApproval", payload.get(RuntimeIntents.AWAIT_EVENT));
        assertEquals("PT24H", payload.get(RuntimeIntents.TIMEOUT_DURATION));
    }

    @Test
    void runDefaultsAwaitEventToApprovalWhenBlank() {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(
                ApprovalTaskHandler.DEFAULT_AWAIT_EVENT,
                r.payload().get(RuntimeIntents.AWAIT_EVENT));
    }

    @Test
    void canAcceptCustomAwaitEventAndTimeout() {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", "myEvent", null);
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("myEvent", null), cfg));
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_timeout", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("approval", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), cfg));
        assertFalse(handler.canAccept(ctx, null, cfg));
    }

    @Test
    void canAcceptDefaultAwaitEvent() {
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("approval", null), null));
    }

    @Test
    void onEventTimeoutYieldsTimeout() {
        NodeResult r =
                handler.onEvent(
                        new TestContext(),
                        new WorkflowEvent("_timeout", null),
                        new ApprovalTaskConfig("u", null, "PT1H"));
        assertEquals(Outcomes.TIMEOUT, r.outcome());
    }

    @Test
    void onEventDecisionMapping() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        assertEquals(
                Outcomes.APPROVED,
                handler.onEvent(
                                new TestContext(),
                                new WorkflowEvent(
                                        "approval", mapper.readTree("{\"decision\":\"approved\"}")),
                                cfg)
                        .outcome());
        assertEquals(
                Outcomes.REJECTED,
                handler.onEvent(
                                new TestContext(),
                                new WorkflowEvent(
                                        "approval", mapper.readTree("{\"decision\":\"rejected\"}")),
                                cfg)
                        .outcome());
        assertEquals(
                Outcomes.SENDBACK,
                handler.onEvent(
                                new TestContext(),
                                new WorkflowEvent(
                                        "approval", mapper.readTree("{\"decision\":\"sendback\"}")),
                                cfg)
                        .outcome());
    }

    @Test
    void onEventMissingDecisionFailsLoudly() {
        NodeResult r =
                handler.onEvent(
                        new TestContext(),
                        new WorkflowEvent("approval", null),
                        new ApprovalTaskConfig("u", null, null));
        assertEquals(NodeStatus.FAILED, r.status());
    }

    @Test
    void canAccept_eventWithoutTaskId_acceptsWhenNameMatches() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        // No payload at all
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("approval", null), cfg));
        // Payload without taskId field
        assertTrue(
                handler.canAccept(
                        ctx,
                        new WorkflowEvent(
                                "approval", mapper.readTree("{\"decision\":\"approved\"}")),
                        cfg));
    }

    @Test
    void canAccept_eventWithTaskIdMatchingNode_accepts() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "approval",
                        mapper.readTree(
                                "{\"taskId\":\"approvals/hrApproval\",\"decision\":\"approved\"}"));
        assertTrue(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_eventWithTaskIdMismatch_rejects() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "approval",
                        mapper.readTree(
                                "{\"taskId\":\"approvals/managerApproval\",\"decision\":\"approved\"}"));
        assertFalse(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_eventWithShortTaskIdEqualToChildId_accepts() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "approval",
                        mapper.readTree("{\"taskId\":\"hrApproval\",\"decision\":\"approved\"}"));
        assertTrue(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_eventWithShortTaskIdMismatch_rejects() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, null);
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "approval",
                        mapper.readTree(
                                "{\"taskId\":\"managerApproval\",\"decision\":\"approved\"}"));
        assertFalse(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_timeoutEventAlwaysAccepted_ignoringTaskId() throws Exception {
        ApprovalTaskConfig cfg = new ApprovalTaskConfig("u", null, "PT1H");
        TestContext ctx = new TestContext().setCurrentNodeId("approvals/hrApproval");
        // _timeout never carries taskId, but even when synthetic payload has one, _timeout wins.
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_timeout", null), cfg));
    }
}
