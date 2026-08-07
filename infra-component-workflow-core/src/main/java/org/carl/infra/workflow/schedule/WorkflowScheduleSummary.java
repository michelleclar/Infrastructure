package org.carl.infra.workflow.schedule;

import java.time.Instant;
import java.util.List;

/** Lightweight schedule view returned by list operations. */
public record WorkflowScheduleSummary(
        String id,
        ScheduleSpec spec,
        ScheduleState state,
        List<ScheduleInfo.ActionResult> recentActions,
        List<Instant> nextActionTimes) {

    public WorkflowScheduleSummary {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (spec == null) {
            throw new NullPointerException("spec");
        }
        if (state == null) {
            throw new NullPointerException("state");
        }
        recentActions = recentActions == null ? List.of() : List.copyOf(recentActions);
        nextActionTimes = nextActionTimes == null ? List.of() : List.copyOf(nextActionTimes);
    }
}
