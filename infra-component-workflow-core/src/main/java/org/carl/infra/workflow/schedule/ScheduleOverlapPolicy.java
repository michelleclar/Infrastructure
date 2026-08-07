package org.carl.infra.workflow.schedule;

/** Controls what happens when a scheduled execution is due while an earlier one is still running. */
public enum ScheduleOverlapPolicy {
    /** Drop the new execution. */
    SKIP,

    /** Keep one pending execution and start it when the running execution finishes. */
    BUFFER_ONE,

    /** Keep every pending execution and run them in order. */
    BUFFER_ALL,

    /** Cancel the running execution before starting the new execution. */
    CANCEL_RUNNING,

    /** Terminate the running execution before starting the new execution. */
    TERMINATE_RUNNING,

    /** Start the new execution concurrently with all running executions. */
    ALLOW_ALL
}
