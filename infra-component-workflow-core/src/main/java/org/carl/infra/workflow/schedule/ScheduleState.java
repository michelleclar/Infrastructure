package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Mutable server-side state represented in a complete schedule definition. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleState(String note, boolean paused, Long remainingActions) {

    public ScheduleState {
        if (note != null && note.isBlank()) {
            note = null;
        }
        if (remainingActions != null && remainingActions < 0) {
            throw new IllegalArgumentException("remainingActions must not be negative");
        }
    }

    /** An active schedule with no execution-count limit. */
    public static ScheduleState active() {
        return new ScheduleState(null, false, null);
    }
}
