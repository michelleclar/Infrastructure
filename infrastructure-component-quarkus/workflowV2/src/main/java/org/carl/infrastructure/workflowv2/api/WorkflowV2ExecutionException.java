package org.carl.infrastructure.workflowv2.api;

/**
 * Thrown when a process instance ends in failure. Temporal-free: callers and tests can catch this
 * without importing {@code io.temporal.*} (the underlying Temporal failure is kept as the cause).
 */
public class WorkflowV2ExecutionException extends RuntimeException {

    public WorkflowV2ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
