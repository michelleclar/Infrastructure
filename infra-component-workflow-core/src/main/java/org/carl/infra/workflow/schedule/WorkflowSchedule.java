package org.carl.infra.workflow.schedule;

import java.util.Objects;

/** Complete runtime-independent definition of a recurring workflow schedule. */
public record WorkflowSchedule(
        String id,
        ScheduledWorkflowAction action,
        ScheduleSpec spec,
        SchedulePolicy policy,
        ScheduleState state) {

    public WorkflowSchedule {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(spec, "spec");
        policy = policy == null ? SchedulePolicy.defaults() : policy;
        state = state == null ? ScheduleState.active() : state;
    }

    /** Creates an active, unlimited schedule with the default overlap policy. */
    public static WorkflowSchedule of(
            String id, ScheduledWorkflowAction action, ScheduleSpec spec) {
        return new WorkflowSchedule(id, action, spec, null, null);
    }
}
