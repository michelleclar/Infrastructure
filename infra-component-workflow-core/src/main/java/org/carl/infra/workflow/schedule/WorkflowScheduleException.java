package org.carl.infra.workflow.schedule;

/** Base exception for distributed workflow-schedule operations. */
public class WorkflowScheduleException extends RuntimeException {
    public WorkflowScheduleException(String message, Throwable cause) {
        super(message, cause);
    }
}
