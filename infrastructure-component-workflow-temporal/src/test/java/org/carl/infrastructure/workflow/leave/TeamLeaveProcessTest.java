package org.carl.infrastructure.workflow.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.api.ApprovalPolicy;
import org.carl.infrastructure.workflow.api.Decision;
import org.carl.infrastructure.workflow.testing.WorkflowTestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * End-to-end tests of the multi-approver (会签) gathering feature on an in-memory, time-skipping
 * Temporal. All tests register two variants of TeamLeaveProcess (allApprove + kOfN(2)) so we can
 * exercise both policies in the same JVM.
 */
class TeamLeaveProcessTest {

    private static final String TASK_QUEUE = "workflow-tasks";
    private static final String P_ALL = "team-leave-all";
    private static final String P_2OF3 = "team-leave-2of3";

    private WorkflowTestKit kit;

    @BeforeEach
    void setUp() {
        TeamRecorder.reset();
        kit =
                WorkflowTestKit.inMemory(
                        TASK_QUEUE,
                        new TeamLeaveProcess(P_ALL, ApprovalPolicy.allApprove()),
                        new TeamLeaveProcess(P_2OF3, ApprovalPolicy.kOfN(2)));
    }

    @AfterEach
    void tearDown() {
        kit.close();
    }

    private static TeamLeaveCtx ctx(String id) {
        return new TeamLeaveCtx(id, List.of("alice", "bob", "carol"));
    }

    @Test
    void all_approve_completes() {
        kit.start(P_ALL, "1", ctx("1"));
        kit.vote(P_ALL, "1", "alice", Decision.APPROVE);
        kit.vote(P_ALL, "1", "bob", Decision.APPROVE);
        kit.vote(P_ALL, "1", "carol", Decision.APPROVE);

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult(P_ALL, "1", LeaveStatus.class));
        assertTrue(TeamRecorder.EVENTS.contains("approved:1"));
    }

    @Test
    void any_reject_rejects_allApprove() {
        kit.start(P_ALL, "2", ctx("2"));
        kit.vote(P_ALL, "2", "alice", Decision.APPROVE);
        kit.vote(P_ALL, "2", "bob", Decision.REJECT);
        // carol's vote never sent; allApprove() short-circuits on the REJECT

        assertEquals(LeaveStatus.REJECTED, kit.awaitResult(P_ALL, "2", LeaveStatus.class));
        assertTrue(TeamRecorder.EVENTS.contains("rejected:2"));
    }

    @Test
    void k_of_n_succeeds_early() {
        kit.start(P_2OF3, "3", ctx("3"));
        kit.vote(P_2OF3, "3", "alice", Decision.APPROVE);
        kit.vote(P_2OF3, "3", "bob", Decision.APPROVE);
        // carol's vote never sent; kOfN(2) reaches APPROVED on the 2nd approve

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult(P_2OF3, "3", LeaveStatus.class));
        assertTrue(TeamRecorder.EVENTS.contains("approved:3"));
    }

    @Test
    void non_assignee_vote_dropped() {
        kit.start(P_ALL, "4", ctx("4"));
        // Random non-assignee — must be ignored
        kit.vote(P_ALL, "4", "eve", Decision.REJECT);
        // Then the real approvers all approve
        kit.vote(P_ALL, "4", "alice", Decision.APPROVE);
        kit.vote(P_ALL, "4", "bob", Decision.APPROVE);
        kit.vote(P_ALL, "4", "carol", Decision.APPROVE);

        assertEquals(LeaveStatus.APPROVED, kit.awaitResult(P_ALL, "4", LeaveStatus.class));
    }

    @Test
    void duplicate_vote_last_wins() {
        kit.start(P_ALL, "5", ctx("5"));
        kit.vote(P_ALL, "5", "alice", Decision.APPROVE);
        kit.vote(P_ALL, "5", "bob", Decision.APPROVE);
        // Carol approves first, then changes mind to REJECT — last write should win → REJECTED
        kit.vote(P_ALL, "5", "carol", Decision.APPROVE);
        kit.vote(P_ALL, "5", "carol", Decision.REJECT);

        assertEquals(LeaveStatus.REJECTED, kit.awaitResult(P_ALL, "5", LeaveStatus.class));
        assertTrue(TeamRecorder.EVENTS.contains("rejected:5"));
    }
}
