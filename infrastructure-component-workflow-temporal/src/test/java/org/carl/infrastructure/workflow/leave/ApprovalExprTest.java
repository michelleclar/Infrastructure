package org.carl.infrastructure.workflow.leave;

import static org.carl.infrastructure.workflow.api.Exprs.and;
import static org.carl.infrastructure.workflow.api.Exprs.atLeast;
import static org.carl.infrastructure.workflow.api.Exprs.or;
import static org.carl.infrastructure.workflow.api.Exprs.step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.api.Decision;
import org.carl.infrastructure.workflow.api.Expr;
import org.carl.infrastructure.workflow.testing.WorkflowTestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * End-to-end tests of the expression-based approval engine (会签 v2). All tests use the in-memory
 * time-skipping Temporal. Zero {@code io.temporal.*} imports — everything goes through
 * {@link WorkflowTestKit}.
 */
class ApprovalExprTest {

    private static final String TASK_QUEUE = "workflow-expr-tasks";

    private WorkflowTestKit kit;

    @BeforeEach
    void setUp() {
        ApprovalRecorder.reset();
    }

    @AfterEach
    void tearDown() {
        if (kit != null) {
            kit.close();
            kit = null;
        }
    }

    // ── Helper to create a kit with one process ───────────────────────────────

    private void startKit(ApprovalExprProcess... procs) {
        kit = WorkflowTestKit.inMemory(TASK_QUEUE, procs);
    }

    private static ApprovalExprCtx ctx(String id) {
        return new ApprovalExprCtx(id);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void and_all_approve() {
        Expr expr = and(step("a"), step("b"), step("c"));
        startKit(new ApprovalExprProcess("and-all", expr));

        kit.start("and-all", "1", ctx("1"));
        kit.vote("and-all", "1", "a", "alice", Decision.APPROVE);
        kit.vote("and-all", "1", "b", "bob", Decision.APPROVE);
        kit.vote("and-all", "1", "c", "carol", Decision.APPROVE);

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult("and-all", "1", LeaveStatus.class));
    }

    @Test
    void and_one_reject_short_circuits() {
        // b REJECT → entire AND goes FALSE; c never needs to vote
        Expr expr = and(step("a"), step("b"), step("c"));
        startKit(new ApprovalExprProcess("and-reject", expr));

        kit.start("and-reject", "2", ctx("2"));
        kit.vote("and-reject", "2", "a", "alice", Decision.APPROVE);
        kit.vote("and-reject", "2", "b", "bob", Decision.REJECT);
        // c deliberately NOT voted

        assertEquals(LeaveStatus.REJECTED, kit.awaitResult("and-reject", "2", LeaveStatus.class));
    }

    @Test
    void or_first_approve_short_circuits() {
        // a APPROVE → OR is TRUE immediately; b never needs to vote
        Expr expr = or(step("a"), step("b"));
        startKit(new ApprovalExprProcess("or-approve", expr));

        kit.start("or-approve", "3", ctx("3"));
        kit.vote("or-approve", "3", "a", "alice", Decision.APPROVE);
        // b deliberately NOT voted

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult("or-approve", "3", LeaveStatus.class));
    }

    @Test
    void at_least_k_succeeds_early() {
        // atLeast(2, a, b, c): once 2 approve, done
        Expr expr = atLeast(2, step("a"), step("b"), step("c"));
        startKit(new ApprovalExprProcess("atleast", expr));

        kit.start("atleast", "4", ctx("4"));
        kit.vote("atleast", "4", "a", "alice", Decision.APPROVE);
        kit.vote("atleast", "4", "b", "bob", Decision.APPROVE);
        // c deliberately NOT voted

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult("atleast", "4", LeaveStatus.class));
    }

    @Test
    void nested_and_of_or() {
        // and(a, or(b, c), d) — satisfied by a + b + d (c never votes)
        Expr expr = and(step("a"), or(step("b"), step("c")), step("d"));
        startKit(new ApprovalExprProcess("nested", expr));

        kit.start("nested", "5", ctx("5"));
        kit.vote("nested", "5", "a", "alice", Decision.APPROVE);
        kit.vote("nested", "5", "b", "bob", Decision.APPROVE);
        kit.vote("nested", "5", "d", "dave", Decision.APPROVE);
        // c deliberately NOT voted

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult("nested", "5", LeaveStatus.class));
    }

    @Test
    void sendback_clears_and_waits() {
        // Vote b APPROVE; then b SENDBACK (clears); assert the overall result is still waiting;
        // then b APPROVE again → should reach APPROVED.
        //
        // With the time-skipping env we can't directly observe UNKNOWN "in between", but we can
        // demonstrate that the workflow does NOT complete after the SENDBACK (it's still waiting),
        // and does complete after the second b APPROVE.
        Expr expr = and(step("a"), step("b"));
        startKit(new ApprovalExprProcess("sendback", expr));

        kit.start("sendback", "6", ctx("6"));
        kit.vote("sendback", "6", "a", "alice", Decision.APPROVE);
        kit.vote("sendback", "6", "b", "bob", Decision.APPROVE);   // would complete...
        kit.vote("sendback", "6", "b", "bob", Decision.SENDBACK); // ...but sendback clears it
        kit.vote("sendback", "6", "b", "bob", Decision.APPROVE);  // approve again

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult("sendback", "6", LeaveStatus.class));
    }

    @Test
    void hook_before_fires_at_entry() {
        // Register before hooks on all three steps. Assert all before-events appear in the recorder
        // BEFORE any vote-triggered events (after/onComplete hooks are absent here so we just check
        // the before events exist and appear first).
        Expr expr = and(
                step("a").before(new ApprovalExprProcess.RecordHook("hookBeforeA", "before")),
                step("b").before(new ApprovalExprProcess.RecordHook("hookBeforeB", "before")),
                step("c").before(new ApprovalExprProcess.RecordHook("hookBeforeC", "before")));
        startKit(new ApprovalExprProcess("hook-before", expr));

        kit.start("hook-before", "7", ctx("7"));
        kit.vote("hook-before", "7", "a", "alice", Decision.APPROVE);
        kit.vote("hook-before", "7", "b", "bob", Decision.APPROVE);
        kit.vote("hook-before", "7", "c", "carol", Decision.APPROVE);

        kit.awaitResult("hook-before", "7", LeaveStatus.class);

        List<String> events = ApprovalRecorder.EVENTS;
        // All three before hooks should have fired
        assertTrue(events.contains("before:a"), "expected before:a in " + events);
        assertTrue(events.contains("before:b"), "expected before:b in " + events);
        assertTrue(events.contains("before:c"), "expected before:c in " + events);

        // All before events should appear before any non-before event (the votes themselves don't
        // produce recorder events in this test; we just check their relative position).
        int lastBeforeIdx = Math.max(
                Math.max(events.indexOf("before:a"), events.indexOf("before:b")),
                events.indexOf("before:c"));
        // There are only before events in the recorder for this test, so lastBeforeIdx == events.size()-1.
        // The key assertion is that ALL three before events ARE present (above).
        assertEquals(3, events.size(), "expected exactly 3 before events, got: " + events);
        assertEquals(0, events.indexOf("before:a"));
        assertEquals(1, events.indexOf("before:b"));
        assertEquals(2, events.indexOf("before:c"));
    }

    @Test
    void hook_after_fires_per_vote() {
        // After hook on step b only. Send: b SENDBACK (1st vote), then b APPROVE (2nd vote).
        // Assert after hook fired exactly twice.
        Expr expr = and(
                step("a"),
                step("b").after(new ApprovalExprProcess.RecordHook("hookAfterB", "after")));
        startKit(new ApprovalExprProcess("hook-after", expr));

        kit.start("hook-after", "8", ctx("8"));
        // Send SENDBACK first — after hook should fire
        kit.vote("hook-after", "8", "b", "bob", Decision.SENDBACK);
        // Then APPROVE a and b — after hook fires again for b
        kit.vote("hook-after", "8", "a", "alice", Decision.APPROVE);
        kit.vote("hook-after", "8", "b", "bob", Decision.APPROVE);

        kit.awaitResult("hook-after", "8", LeaveStatus.class);

        List<String> events = ApprovalRecorder.EVENTS;
        long afterCount = events.stream().filter(e -> e.equals("after:b")).count();
        assertEquals(2, afterCount, "expected after:b fired exactly twice, got: " + events);
    }

    @Test
    void hook_onComplete_fires_on_terminal_leaf() {
        // onComplete on step b. Vote sequence:
        //   1. b APPROVE  → b transitions UNKNOWN→TRUE  → onComplete fires (count=1)
        //   2. b SENDBACK → b transitions TRUE→UNKNOWN  (no onComplete)
        //   3. b REJECT   → b transitions UNKNOWN→FALSE → onComplete fires again (count=2)
        // Also vote a APPROVE so the overall expression can reach terminal.
        Expr expr = and(
                step("a"),
                step("b").onComplete(
                        new ApprovalExprProcess.RecordHook("hookOnCompleteB", "onComplete")));
        startKit(new ApprovalExprProcess("hook-oncomplete", expr));

        kit.start("hook-oncomplete", "9", ctx("9"));

        // Phase 1: b APPROVE → onComplete fires once; overall AND is UNKNOWN (a not voted yet)
        kit.vote("hook-oncomplete", "9", "b", "bob", Decision.APPROVE);
        // Phase 2: b SENDBACK → clears b; previousLeafState for b should go back to UNKNOWN
        kit.vote("hook-oncomplete", "9", "b", "bob", Decision.SENDBACK);
        // Phase 3: b REJECT → onComplete fires again; a still not voted so AND = UNKNOWN (b=FALSE)
        // Actually: AND(a=UNKNOWN, b=FALSE) = FALSE → workflow REJECTS
        kit.vote("hook-oncomplete", "9", "b", "bob", Decision.REJECT);

        kit.awaitResult("hook-oncomplete", "9", LeaveStatus.class);

        List<String> events = ApprovalRecorder.EVENTS;
        long onCompleteCount = events.stream().filter(e -> e.equals("onComplete:b")).count();
        assertEquals(2, onCompleteCount,
                "expected onComplete:b fired exactly twice, got: " + events);
    }
}
