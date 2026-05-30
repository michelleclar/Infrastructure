package org.carl.infrastructure.workflowv2.leave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory test sink. Activities run in the same JVM as the test under {@code
 * TestWorkflowEnvironment}, so static state is visible to assertions.
 */
public final class Recorder {

    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();
    public static final AtomicInteger DEDUCT_ATTEMPTS = new AtomicInteger();

    /** Number of leading deductBalance attempts that should fail (to exercise retry). */
    public static final AtomicInteger deductFailTimes = new AtomicInteger();

    /** When true, writeRecord throws (to exercise compensation). */
    public static final AtomicBoolean failWriteRecord = new AtomicBoolean();

    private Recorder() {}

    public static void reset() {
        EVENTS.clear();
        DEDUCT_ATTEMPTS.set(0);
        deductFailTimes.set(0);
        failWriteRecord.set(false);
    }
}
