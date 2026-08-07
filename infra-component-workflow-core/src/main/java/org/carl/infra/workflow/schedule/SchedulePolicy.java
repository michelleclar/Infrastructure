package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.util.Objects;

/** Failure recovery and overlap behavior for recurring workflow executions. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SchedulePolicy(
        ScheduleOverlapPolicy overlapPolicy, Duration catchupWindow, boolean pauseOnFailure) {

    public SchedulePolicy {
        Objects.requireNonNull(overlapPolicy, "overlapPolicy");
        if (catchupWindow != null && (catchupWindow.isZero() || catchupWindow.isNegative())) {
            throw new IllegalArgumentException("catchupWindow must be positive");
        }
    }

    /** Default policy: skip overlapping executions and use the runtime's catch-up window. */
    public static SchedulePolicy defaults() {
        return new SchedulePolicy(ScheduleOverlapPolicy.SKIP, null, false);
    }
}
