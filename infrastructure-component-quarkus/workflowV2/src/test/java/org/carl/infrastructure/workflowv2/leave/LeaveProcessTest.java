package org.carl.infrastructure.workflowv2.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflowv2.api.WorkflowV2ExecutionException;
import org.carl.infrastructure.workflowv2.testing.WorkflowV2TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests of the generic engine on an in-memory, time-skipping Temporal (no real server,
 * no DB). Note: <b>zero {@code io.temporal.*} imports</b> — everything goes through {@link
 * WorkflowV2TestKit}. Covers orchestration, nodes, await-timeout, retry, and Saga compensation.
 */
class LeaveProcessTest {

    private static final String TASK_QUEUE = "workflowv2-tasks";

    private WorkflowV2TestKit kit;

    @BeforeEach
    void setUp() {
        Recorder.reset();
        kit = WorkflowV2TestKit.inMemory(TASK_QUEUE, new LeaveProcess());
    }

    @AfterEach
    void tearDown() {
        kit.close();
    }

    @Test
    void approves_and_completes() {
        kit.start("leave", "normal", new LeaveCtx("1", "boss"));
        kit.signal("leave", "normal", LeaveEvent.APPROVE);
        kit.signal("leave", "normal", LeaveEvent.CONFIRM);

        assertEquals(LeaveStatus.DONE, kit.awaitResult("leave", "normal", LeaveStatus.class));
        assertTrue(Recorder.EVENTS.contains("deductBalance"));
        assertTrue(Recorder.EVENTS.contains("writeRecord"));
    }

    @Test
    void auto_rejects_on_timeout() {
        // no signal -> SUBMITTED await (24h) elapses; the time-skipping env advances automatically
        kit.start("leave", "timeout", new LeaveCtx("2", "boss"));

        assertEquals(LeaveStatus.REJECTED, kit.awaitResult("leave", "timeout", LeaveStatus.class));
    }

    @Test
    void compensates_when_a_later_node_fails() {
        Recorder.failWriteRecord.set(true);
        kit.start("leave", "comp", new LeaveCtx("3", "boss"));
        kit.signal("leave", "comp", LeaveEvent.APPROVE);
        kit.signal("leave", "comp", LeaveEvent.CONFIRM);

        assertThrows(
                WorkflowV2ExecutionException.class,
                () -> kit.awaitResult("leave", "comp", LeaveStatus.class));
        assertTrue(Recorder.EVENTS.contains("deductBalance"));
        // compensation ran AND knew the transition it was rolling back (APPROVED via APPROVE)
        assertTrue(Recorder.EVENTS.contains("restoreBalance:APPROVED:APPROVE"));
        assertFalse(Recorder.EVENTS.contains("writeRecord"));
    }

    @Test
    void retries_a_failing_node_until_it_succeeds() {
        Recorder.deductFailTimes.set(1); // fail once, succeed on the 2nd attempt
        kit.start("leave", "retry", new LeaveCtx("4", "boss"));
        kit.signal("leave", "retry", LeaveEvent.APPROVE);
        kit.signal("leave", "retry", LeaveEvent.CONFIRM);

        assertEquals(LeaveStatus.DONE, kit.awaitResult("leave", "retry", LeaveStatus.class));
        assertEquals(2, Recorder.DEDUCT_ATTEMPTS.get());
    }
}
