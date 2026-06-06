package org.carl.infrastructure.workflow.spi;

/**
 * Canonical outcome constants emitted by built-in node handlers.
 *
 * <p>Stable English string constants used by edge routing.
 */
public final class Outcomes {

    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String SENDBACK = "SENDBACK";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RECEIVED = "RECEIVED";
    public static final String TRIGGERED = "TRIGGERED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    private Outcomes() {
        throw new AssertionError("no instances");
    }
}
