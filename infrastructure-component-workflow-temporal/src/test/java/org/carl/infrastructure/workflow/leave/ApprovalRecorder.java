package org.carl.infrastructure.workflow.leave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Static event log for the expression-based approval tests. Activities append event strings here;
 * tests assert on the log. Thread-safe: activities run on Temporal worker threads.
 */
public final class ApprovalRecorder {

    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    private ApprovalRecorder() {}

    public static void reset() {
        EVENTS.clear();
    }
}
