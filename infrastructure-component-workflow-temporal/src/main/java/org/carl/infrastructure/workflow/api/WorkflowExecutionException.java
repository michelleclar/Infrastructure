package org.carl.infrastructure.workflow.api;

/**
 * Thrown when a process instance ends in failure. Temporal-free: callers and tests can catch this
 * without importing {@code io.temporal.*} (the underlying Temporal failure is kept as the cause).
 */
public class WorkflowExecutionException extends RuntimeException {

    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
