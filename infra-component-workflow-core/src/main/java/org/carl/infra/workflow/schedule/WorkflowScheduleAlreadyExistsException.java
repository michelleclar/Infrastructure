package org.carl.infra.workflow.schedule;

/** Raised when creating a schedule whose identifier already exists. */
public final class WorkflowScheduleAlreadyExistsException extends WorkflowScheduleException {
    public WorkflowScheduleAlreadyExistsException(String scheduleId, Throwable cause) {
        super("workflow schedule already exists: " + scheduleId, cause);
    }
}
