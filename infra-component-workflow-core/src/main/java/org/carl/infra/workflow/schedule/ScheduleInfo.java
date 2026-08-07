package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Runtime information maintained by the distributed scheduler. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduleInfo(
        long actionCount,
        long missedCatchupWindowCount,
        long skippedOverlapCount,
        List<Execution> runningActions,
        List<ActionResult> recentActions,
        List<Instant> nextActionTimes,
        Instant createdAt,
        Instant lastUpdatedAt) {

    public ScheduleInfo {
        if (actionCount < 0 || missedCatchupWindowCount < 0 || skippedOverlapCount < 0) {
            throw new IllegalArgumentException("schedule counters must not be negative");
        }
        runningActions = immutable(runningActions);
        recentActions = immutable(recentActions);
        nextActionTimes = immutable(nextActionTimes);
    }

    /** Identifies a workflow execution started by the schedule. */
    public record Execution(String workflowId, String firstExecutionRunId) {
        public Execution {
            requireText(workflowId, "workflowId");
            requireText(firstExecutionRunId, "firstExecutionRunId");
        }
    }

    /** Timing and execution identity for a completed or recently started action. */
    public record ActionResult(Instant scheduledAt, Instant startedAt, Execution execution) {
        public ActionResult {
            Objects.requireNonNull(scheduledAt, "scheduledAt");
            Objects.requireNonNull(startedAt, "startedAt");
            Objects.requireNonNull(execution, "execution");
        }
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
