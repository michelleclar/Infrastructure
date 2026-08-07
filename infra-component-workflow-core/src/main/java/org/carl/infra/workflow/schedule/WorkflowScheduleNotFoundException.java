package org.carl.infra.workflow.schedule;

/** Raised when a schedule operation addresses an identifier that does not exist. */
public final class WorkflowScheduleNotFoundException extends WorkflowScheduleException {
    public WorkflowScheduleNotFoundException(String scheduleId, Throwable cause) {
        super("workflow schedule not found: " + scheduleId, cause);
    }
}
