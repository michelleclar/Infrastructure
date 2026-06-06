package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.JoinRule;
import org.carl.infrastructure.workflow.handlers.TaskGroupConfig.TaskGroupChild;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class TaskGroupHandlerTest {

    private final TaskGroupHandler handler = new TaskGroupHandler();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.TASK_GROUP, handler.type());
        assertEquals(TaskGroupConfig.class, handler.configType());
        // outcomes is the union of possible child outcomes.
        assertTrue(handler.outcomes().contains(Outcomes.APPROVED));
        assertTrue(handler.outcomes().contains(Outcomes.REJECTED));
        assertTrue(handler.outcomes().contains(Outcomes.SENDBACK));
        assertTrue(handler.outcomes().contains(Outcomes.TIMEOUT));
    }

    @Test
    void runEncodesChildrenAndJoinRule() {
        TaskGroupConfig cfg =
                new TaskGroupConfig(
                        JoinRule.ALL,
                        List.of(
                                new TaskGroupChild("hr", "HR", "approvalTask", null),
                                new TaskGroupChild("mgr", "Mgr", "approvalTask", null)));
        NodeResult r = handler.run(new TestContext().setCurrentNodeId("parent"), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
        Map<String, Object> p = r.payload();
        assertEquals("all", p.get(RuntimeIntents.JOIN_RULE));
        Object children = p.get(RuntimeIntents.CHILDREN);
        assertNotNull(children);
        assertTrue(children instanceof List<?>);
        assertEquals(2, ((List<?>) children).size());
    }

    @Test
    void runDefaultsJoinRuleToAll() {
        TaskGroupConfig cfg =
                new TaskGroupConfig(
                        null, List.of(new TaskGroupChild("c", null, "approvalTask", null)));
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals("all", r.payload().get(RuntimeIntents.JOIN_RULE));
    }

    @Test
    void canAcceptChildCompletedEvent() {
        TestContext ctx = new TestContext();
        assertTrue(handler.canAccept(ctx, new WorkflowEvent("_childCompleted", null), null));
        assertFalse(handler.canAccept(ctx, new WorkflowEvent("other", null), null));
    }

    @Test
    void allRule_twoApprovedYieldsApproved() {
        TaskGroupConfig cfg = group(JoinRule.ALL, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.APPROVED));
        ctx.setResult(
                TaskGroupContract.childKey("parent", "b"), NodeResult.completed(Outcomes.APPROVED));
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(Outcomes.APPROVED, r.outcome());
    }

    @Test
    void allRule_oneRejectedShortCircuitsRejected() {
        TaskGroupConfig cfg = group(JoinRule.ALL, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.APPROVED));
        ctx.setResult(
                TaskGroupContract.childKey("parent", "b"), NodeResult.completed(Outcomes.REJECTED));
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(Outcomes.REJECTED, r.outcome());
    }

    @Test
    void allRule_oneApprovedOneStillRunningStaysWaiting() {
        TaskGroupConfig cfg = group(JoinRule.ALL, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.APPROVED));
        // child b not yet seeded.
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
    }

    @Test
    void allRule_anySendbackWinsOverApprovals() {
        TaskGroupConfig cfg = group(JoinRule.ALL, "a", "b", "c");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.APPROVED));
        ctx.setResult(
                TaskGroupContract.childKey("parent", "b"), NodeResult.completed(Outcomes.SENDBACK));
        ctx.setResult(
                TaskGroupContract.childKey("parent", "c"), NodeResult.completed(Outcomes.APPROVED));
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(Outcomes.SENDBACK, r.outcome());
    }

    @Test
    void anyRule_firstApprovedWins() {
        TaskGroupConfig cfg = group(JoinRule.ANY, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.APPROVED));
        // b still pending; ANY should fire immediately on first approval.
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(Outcomes.APPROVED, r.outcome());
    }

    @Test
    void anyRule_allRejectedYieldsRejected() {
        TaskGroupConfig cfg = group(JoinRule.ANY, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.REJECTED));
        ctx.setResult(
                TaskGroupContract.childKey("parent", "b"), NodeResult.completed(Outcomes.REJECTED));
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(Outcomes.REJECTED, r.outcome());
    }

    @Test
    void anyRule_pendingStillWaiting() {
        TaskGroupConfig cfg = group(JoinRule.ANY, "a", "b");
        TestContext ctx = parentCtx();
        ctx.setResult(
                TaskGroupContract.childKey("parent", "a"), NodeResult.completed(Outcomes.REJECTED));
        // b still pending.
        NodeResult r = handler.onEvent(ctx, new WorkflowEvent("_childCompleted", null), cfg);
        assertEquals(NodeStatus.WAITING, r.status());
    }

    @Test
    void aggregateOnApproveSuccessCompletedAllCount() {
        // success and completed are also "approved-like".
        NodeResult r =
                TaskGroupHandler.aggregate(
                        JoinRule.ALL,
                        Arrays.asList(
                                NodeResult.completed(Outcomes.SUCCESS),
                                NodeResult.completed(Outcomes.COMPLETED)));
        assertEquals(Outcomes.APPROVED, r.outcome());
    }

    @Test
    void emptyChildrenListImmediatelyApproved() {
        NodeResult r = TaskGroupHandler.aggregate(JoinRule.ALL, Collections.emptyList());
        assertEquals(Outcomes.APPROVED, r.outcome());
    }

    @Test
    void childKeyConcatenatesParentAndChild() {
        assertEquals("parent/child", TaskGroupContract.childKey("parent", "child"));
        assertThrows(IllegalArgumentException.class, () -> TaskGroupContract.childKey("", "c"));
        assertThrows(IllegalArgumentException.class, () -> TaskGroupContract.childKey("p", ""));
    }

    private static TaskGroupConfig group(JoinRule rule, String... ids) {
        return new TaskGroupConfig(
                rule,
                Arrays.stream(ids)
                        .map(id -> new TaskGroupChild(id, null, "approvalTask", null))
                        .toList());
    }

    private static TestContext parentCtx() {
        return new TestContext().setCurrentNodeId("parent");
    }
}
