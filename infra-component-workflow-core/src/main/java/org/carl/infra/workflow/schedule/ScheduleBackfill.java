package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Objects;

/** Requests executions for matching schedule times in a historical time range. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleBackfill(
        Instant startExclusive, Instant endInclusive, ScheduleOverlapPolicy overlapPolicy) {

    public ScheduleBackfill {
        Objects.requireNonNull(startExclusive, "startExclusive");
        Objects.requireNonNull(endInclusive, "endInclusive");
        if (!endInclusive.isAfter(startExclusive)) {
            throw new IllegalArgumentException("endInclusive must be after startExclusive");
        }
    }

    /** Uses the schedule's configured overlap policy. */
    public ScheduleBackfill(Instant startExclusive, Instant endInclusive) {
        this(startExclusive, endInclusive, null);
    }
}
