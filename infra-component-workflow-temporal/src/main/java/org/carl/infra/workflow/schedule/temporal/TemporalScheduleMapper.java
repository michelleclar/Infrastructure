package org.carl.infra.workflow.schedule.temporal;

import com.fasterxml.jackson.core.type.TypeReference;

import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.ScheduleActionExecution;
import io.temporal.client.schedules.ScheduleActionExecutionStartWorkflow;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleCalendarSpec;
import io.temporal.client.schedules.ScheduleDescription;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import io.temporal.client.schedules.ScheduleListDescription;
import io.temporal.client.schedules.ScheduleListInfo;
import io.temporal.client.schedules.ScheduleListSchedule;
import io.temporal.client.schedules.ScheduleListState;
import io.temporal.client.schedules.ScheduleRange;

import org.carl.infra.workflow.runtime.GenericWorkflow;
import org.carl.infra.workflow.runtime.ObjectMapperHolder;
import org.carl.infra.workflow.runtime.WorkflowInput;
import org.carl.infra.workflow.schedule.ScheduleBackfill;
import org.carl.infra.workflow.schedule.ScheduleInfo;
import org.carl.infra.workflow.schedule.SchedulePolicy;
import org.carl.infra.workflow.schedule.ScheduleSpec;
import org.carl.infra.workflow.schedule.ScheduleState;
import org.carl.infra.workflow.schedule.ScheduledWorkflowAction;
import org.carl.infra.workflow.schedule.WorkflowSchedule;
import org.carl.infra.workflow.schedule.WorkflowScheduleDescription;
import org.carl.infra.workflow.schedule.WorkflowScheduleSummary;

import java.util.List;
import java.util.Map;

/** Exact type mapping between workflow-core schedule contracts and the Temporal Java SDK. */
final class TemporalScheduleMapper {

    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECT =
            new TypeReference<>() {};

    private TemporalScheduleMapper() {}

    static io.temporal.client.schedules.Schedule toTemporal(
            WorkflowSchedule source, String taskQueue) {
        ScheduledWorkflowAction action = source.action();
        WorkflowInput input =
                WorkflowInput.from(
                        action.definition(),
                        action.businessData(),
                        action.initialVariables(),
                        action.startNodeId(),
                        action.archive());

        ScheduleActionStartWorkflow temporalAction =
                ScheduleActionStartWorkflow.newBuilder()
                        .setWorkflowType(GenericWorkflow.class)
                        .setOptions(
                                WorkflowOptions.newBuilder()
                                        .setWorkflowId(action.workflowId())
                                        .setTaskQueue(taskQueue)
                                        .build())
                        .setArguments(input)
                        .build();

        return io.temporal.client.schedules.Schedule.newBuilder()
                .setAction(temporalAction)
                .setSpec(toTemporal(source.spec()))
                .setPolicy(toTemporal(source.policy()))
                .setState(toTemporal(source.state()))
                .build();
    }

    static io.temporal.client.schedules.ScheduleSpec toTemporal(ScheduleSpec source) {
        return io.temporal.client.schedules.ScheduleSpec.newBuilder()
                .setCalendars(source.calendars().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setIntervals(source.intervals().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setCronExpressions(source.cronExpressions())
                .setSkip(source.skipCalendars().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setStartAt(source.startAt())
                .setEndAt(source.endAt())
                .setJitter(source.jitter())
                .setTimeZoneName(source.timeZoneName())
                .build();
    }

    static io.temporal.client.schedules.SchedulePolicy toTemporal(SchedulePolicy source) {
        return io.temporal.client.schedules.SchedulePolicy.newBuilder()
                .setOverlap(toTemporal(source.overlapPolicy()))
                .setCatchupWindow(source.catchupWindow())
                .setPauseOnFailure(source.pauseOnFailure())
                .build();
    }

    static io.temporal.client.schedules.ScheduleState toTemporal(ScheduleState source) {
        io.temporal.client.schedules.ScheduleState.Builder builder =
                io.temporal.client.schedules.ScheduleState.newBuilder()
                        .setNote(source.note())
                        .setPaused(source.paused());
        if (source.remainingActions() != null) {
            builder.setLimitedAction(true).setRemainingActions(source.remainingActions());
        }
        return builder.build();
    }

    static io.temporal.client.schedules.ScheduleBackfill toTemporal(ScheduleBackfill source) {
        if (source.overlapPolicy() == null) {
            return new io.temporal.client.schedules.ScheduleBackfill(
                    source.startExclusive(), source.endInclusive());
        }
        return new io.temporal.client.schedules.ScheduleBackfill(
                source.startExclusive(),
                source.endInclusive(),
                toTemporal(source.overlapPolicy()));
    }

    static WorkflowScheduleDescription fromTemporal(ScheduleDescription source) {
        io.temporal.client.schedules.Schedule temporalSchedule = source.getSchedule();
        if (!(temporalSchedule.getAction() instanceof ScheduleActionStartWorkflow temporalAction)) {
            throw new IllegalArgumentException(
                    "schedule '" + source.getId() + "' does not start a workflow");
        }

        WorkflowInput input = temporalAction.getArguments().get(0, WorkflowInput.class);
        Map<String, Object> businessData =
                input.businessData() == null
                        ? Map.of()
                        : ObjectMapperHolder.mapper().convertValue(input.businessData(), MAP_OF_OBJECT);
        ScheduledWorkflowAction action =
                new ScheduledWorkflowAction(
                        temporalAction.getOptions().getWorkflowId(),
                        input.workflowDefinition(),
                        businessData,
                        input.initialVariables(),
                        input.startNodeId(),
                        input.archiveEnabled());

        WorkflowSchedule schedule =
                new WorkflowSchedule(
                        source.getId(),
                        action,
                        fromTemporal(temporalSchedule.getSpec()),
                        fromTemporal(temporalSchedule.getPolicy()),
                        fromTemporal(temporalSchedule.getState()));

        return new WorkflowScheduleDescription(schedule, fromTemporal(source.getInfo()));
    }

    static WorkflowScheduleSummary fromTemporal(ScheduleListDescription source) {
        ScheduleListSchedule schedule = source.getSchedule();
        ScheduleListState state = schedule.getState();
        ScheduleListInfo info = source.getInfo();
        return new WorkflowScheduleSummary(
                source.getScheduleId(),
                fromTemporal(schedule.getSpec()),
                state == null
                        ? ScheduleState.active()
                        : new ScheduleState(state.getNote(), state.isPaused(), null),
                info == null
                        ? List.of()
                        : info.getRecentActions().stream()
                                .map(TemporalScheduleMapper::fromTemporal)
                                .toList(),
                info == null ? List.of() : info.getNextActionTimes());
    }

    static ScheduleSpec fromTemporal(io.temporal.client.schedules.ScheduleSpec source) {
        return new ScheduleSpec(
                safe(source.getCalendars()).stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                safe(source.getIntervals()).stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                safe(source.getCronExpressions()),
                safe(source.getSkip()).stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getStartAt(),
                source.getEndAt(),
                source.getJitter(),
                blankToNull(source.getTimeZoneName()));
    }

    static SchedulePolicy fromTemporal(io.temporal.client.schedules.SchedulePolicy source) {
        if (source == null) {
            return SchedulePolicy.defaults();
        }
        return new SchedulePolicy(
                fromTemporal(source.getOverlap()),
                source.getCatchupWindow(),
                source.isPauseOnFailure());
    }

    static ScheduleState fromTemporal(io.temporal.client.schedules.ScheduleState source) {
        if (source == null) {
            return ScheduleState.active();
        }
        Long remainingActions = source.isLimitedAction() ? source.getRemainingActions() : null;
        return new ScheduleState(blankToNull(source.getNote()), source.isPaused(), remainingActions);
    }

    static ScheduleInfo fromTemporal(io.temporal.client.schedules.ScheduleInfo source) {
        return new ScheduleInfo(
                source.getNumActions(),
                source.getNumActionsMissedCatchupWindow(),
                source.getNumActionsSkippedOverlap(),
                source.getRunningActions().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getRecentActions().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getNextActionTimes(),
                source.getCreatedAt(),
                source.getLastUpdatedAt());
    }

    static ScheduleOverlapPolicy toTemporal(
            org.carl.infra.workflow.schedule.ScheduleOverlapPolicy source) {
        return switch (source) {
            case SKIP -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP;
            case BUFFER_ONE -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_BUFFER_ONE;
            case BUFFER_ALL -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_BUFFER_ALL;
            case CANCEL_RUNNING -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_CANCEL_OTHER;
            case TERMINATE_RUNNING -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_TERMINATE_OTHER;
            case ALLOW_ALL -> ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL;
        };
    }

    private static org.carl.infra.workflow.schedule.ScheduleOverlapPolicy fromTemporal(
            ScheduleOverlapPolicy source) {
        return switch (source) {
            case SCHEDULE_OVERLAP_POLICY_UNSPECIFIED, SCHEDULE_OVERLAP_POLICY_SKIP ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.SKIP;
            case SCHEDULE_OVERLAP_POLICY_BUFFER_ONE ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.BUFFER_ONE;
            case SCHEDULE_OVERLAP_POLICY_BUFFER_ALL ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.BUFFER_ALL;
            case SCHEDULE_OVERLAP_POLICY_CANCEL_OTHER ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.CANCEL_RUNNING;
            case SCHEDULE_OVERLAP_POLICY_TERMINATE_OTHER ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.TERMINATE_RUNNING;
            case SCHEDULE_OVERLAP_POLICY_ALLOW_ALL ->
                    org.carl.infra.workflow.schedule.ScheduleOverlapPolicy.ALLOW_ALL;
            case UNRECOGNIZED -> throw new IllegalArgumentException("unrecognized overlap policy");
        };
    }

    private static ScheduleIntervalSpec toTemporal(ScheduleSpec.Interval source) {
        return new ScheduleIntervalSpec(source.every(), source.offset());
    }

    private static ScheduleSpec.Interval fromTemporal(ScheduleIntervalSpec source) {
        return new ScheduleSpec.Interval(source.getEvery(), source.getOffset());
    }

    private static ScheduleCalendarSpec toTemporal(ScheduleSpec.Calendar source) {
        return ScheduleCalendarSpec.newBuilder()
                .setSeconds(source.seconds().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setMinutes(source.minutes().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setHour(source.hours().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setDayOfMonth(source.daysOfMonth().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setMonth(source.months().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setYear(source.years().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setDayOfWeek(source.daysOfWeek().stream().map(TemporalScheduleMapper::toTemporal).toList())
                .setComment(source.comment())
                .build();
    }

    private static ScheduleSpec.Calendar fromTemporal(ScheduleCalendarSpec source) {
        return new ScheduleSpec.Calendar(
                source.getSeconds().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getMinutes().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getHour().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getDayOfMonth().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getMonth().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getYear().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                source.getDayOfWeek().stream().map(TemporalScheduleMapper::fromTemporal).toList(),
                blankToNull(source.getComment()));
    }

    private static ScheduleRange toTemporal(ScheduleSpec.Range source) {
        return new ScheduleRange(source.start(), source.end(), source.step());
    }

    private static ScheduleSpec.Range fromTemporal(ScheduleRange source) {
        return new ScheduleSpec.Range(source.getStart(), source.getEnd(), source.getStep());
    }

    private static ScheduleInfo.Execution fromTemporal(ScheduleActionExecution source) {
        if (!(source instanceof ScheduleActionExecutionStartWorkflow workflow)) {
            throw new IllegalArgumentException("schedule action is not a workflow execution");
        }
        return new ScheduleInfo.Execution(
                workflow.getWorkflowId(), workflow.getFirstExecutionRunId());
    }

    private static ScheduleInfo.ActionResult fromTemporal(
            io.temporal.client.schedules.ScheduleActionResult source) {
        return new ScheduleInfo.ActionResult(
                source.getScheduledAt(), source.getStartedAt(), fromTemporal(source.getAction()));
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
