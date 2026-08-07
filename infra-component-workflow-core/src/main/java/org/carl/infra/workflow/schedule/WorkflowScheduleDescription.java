package org.carl.infra.workflow.schedule;

import java.util.Objects;

/** A complete schedule definition plus its server-maintained runtime information. */
public record WorkflowScheduleDescription(WorkflowSchedule schedule, ScheduleInfo info) {
    public WorkflowScheduleDescription {
        Objects.requireNonNull(schedule, "schedule");
        Objects.requireNonNull(info, "info");
    }
}
