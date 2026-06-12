package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

class UserTaskHandlerTest {

    private final UserTaskHandler handler = new UserTaskHandler();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.USER_TASK, handler.type());
        assertEquals(UserTaskConfig.class, handler.configType());
    }

    @Test
    void runPopulatesAssigneeAndAwait() {
        UserTaskConfig cfg = new UserTaskConfig("u", "doIt", "PT5M");
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
        Map<String, Object> p = r.payload();
        assertEquals("u", p.get(RuntimeIntents.ASSIGNEE));
        assertEquals("doIt", p.get(RuntimeIntents.AWAIT_EVENT));
        assertEquals("PT5M", p.get(RuntimeIntents.TIMEOUT_DURATION));
    }

    @Test
    void canAcceptCustomAwaitOrTimeout() {
        UserTaskConfig cfg = new UserTaskConfig(null, "doIt", null);
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("doIt", null), cfg));
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_timeout", null), cfg));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), cfg));
    }

    @Test
    void canAccept_eventWithoutTaskId_acceptsWhenNameMatches() throws Exception {
        UserTaskConfig cfg = new UserTaskConfig(null, "review", null);
        TestContext ctx = new TestContext().setCurrentNodeId("group/childA");
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("review", null), cfg));
        assertTrue(
                handler.canAccept(
                        ctx,
                        new WorkflowEvent(
                                "review", mapper.readTree("{\"decision\":\"completed\"}")),
                        cfg));
    }

    @Test
    void canAccept_eventWithTaskIdMatchingNode_accepts() throws Exception {
        UserTaskConfig cfg = new UserTaskConfig(null, "review", null);
        TestContext ctx = new TestContext().setCurrentNodeId("group/childA");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "review",
                        mapper.readTree(
                                "{\"taskId\":\"group/childA\",\"decision\":\"completed\"}"));
        assertTrue(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_eventWithTaskIdMismatch_rejects() throws Exception {
        UserTaskConfig cfg = new UserTaskConfig(null, "review", null);
        TestContext ctx = new TestContext().setCurrentNodeId("group/childA");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "review",
                        mapper.readTree(
                                "{\"taskId\":\"group/childB\",\"decision\":\"completed\"}"));
        assertFalse(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void canAccept_shortTaskIdEqualToChildId_accepts() throws Exception {
        UserTaskConfig cfg = new UserTaskConfig(null, "review", null);
        TestContext ctx = new TestContext().setCurrentNodeId("group/childA");
        WorkflowEvent ev =
                new WorkflowEvent(
                        "review",
                        mapper.readTree("{\"taskId\":\"childA\",\"decision\":\"completed\"}"));
        assertTrue(handler.canAccept(ctx, ev, cfg));
    }

    @Test
    void onEventCompletedAndCancelled() throws Exception {
        UserTaskConfig cfg = new UserTaskConfig(null, null, null);
        NodeResult done =
                handler.onEvent(
                        new TestContext(),
                        new WorkflowEvent(
                                UserTaskHandler.DEFAULT_AWAIT_EVENT,
                                mapper.readTree("{\"decision\":\"completed\"}")),
                        cfg);
        assertEquals("COMPLETED", done.outcome());

        NodeResult cancelled =
                handler.onEvent(
                        new TestContext(),
                        new WorkflowEvent(
                                UserTaskHandler.DEFAULT_AWAIT_EVENT,
                                mapper.readTree("{\"decision\":\"cancelled\"}")),
                        cfg);
        assertEquals("CANCELLED", cancelled.outcome());

        NodeResult timeout =
                handler.onEvent(new TestContext(), new WorkflowEvent("_timeout", null), cfg);
        assertEquals("TIMEOUT", timeout.outcome());
    }
}
