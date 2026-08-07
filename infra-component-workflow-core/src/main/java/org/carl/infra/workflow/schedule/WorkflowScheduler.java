package org.carl.infra.workflow.schedule;

import java.util.List;

/** Runtime-neutral management API for recurring workflow executions. */
public interface WorkflowScheduler {

    /** Creates a schedule without an immediate trigger or historical backfill. */
    default WorkflowScheduleDescription create(WorkflowSchedule schedule) {
        return create(schedule, ScheduleCreateOptions.defaults());
    }

    /** Creates a schedule with one-time creation behavior. */
    WorkflowScheduleDescription create(
            WorkflowSchedule schedule, ScheduleCreateOptions createOptions);

    /** Returns the complete current schedule definition and runtime information. */
    WorkflowScheduleDescription describe(String scheduleId);

    /** Lists lightweight views of every schedule visible to this scheduler. */
    List<WorkflowScheduleSummary> list();

    /** Atomically replaces the complete schedule definition. */
    WorkflowScheduleDescription update(WorkflowSchedule schedule);

    /** Pauses automatic schedule actions. */
    void pause(String scheduleId, String note);

    /** Resumes automatic schedule actions. */
    void resume(String scheduleId, String note);

    /** Triggers one action immediately using the schedule's overlap policy. */
    void trigger(String scheduleId);

    /** Triggers one action immediately with an explicit overlap-policy override. */
    void trigger(String scheduleId, ScheduleOverlapPolicy overlapPolicy);

    /** Starts actions for matching times inside the supplied historical ranges. */
    void backfill(String scheduleId, List<ScheduleBackfill> backfills);

    /** Permanently deletes the schedule. Running workflow executions are unaffected. */
    void delete(String scheduleId);
}
