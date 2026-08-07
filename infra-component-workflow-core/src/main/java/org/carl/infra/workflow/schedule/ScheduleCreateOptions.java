package org.carl.infra.workflow.schedule;

import java.util.List;

/** One-time behavior applied while a schedule is created. */
public record ScheduleCreateOptions(boolean triggerImmediately, List<ScheduleBackfill> backfills) {

    public ScheduleCreateOptions {
        backfills = backfills == null ? List.of() : List.copyOf(backfills);
    }

    public static ScheduleCreateOptions defaults() {
        return new ScheduleCreateOptions(false, List.of());
    }
}
