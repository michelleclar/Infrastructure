package org.carl.infra.workflow.schedule.temporal;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.schedules.ScheduleAlreadyRunningException;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.ScheduleUpdate;

import org.carl.infra.workflow.schedule.ScheduleBackfill;
import org.carl.infra.workflow.schedule.ScheduleCreateOptions;
import org.carl.infra.workflow.schedule.ScheduleOverlapPolicy;
import org.carl.infra.workflow.schedule.WorkflowSchedule;
import org.carl.infra.workflow.schedule.WorkflowScheduleAlreadyExistsException;
import org.carl.infra.workflow.schedule.WorkflowScheduleDescription;
import org.carl.infra.workflow.schedule.WorkflowScheduleException;
import org.carl.infra.workflow.schedule.WorkflowScheduleNotFoundException;
import org.carl.infra.workflow.schedule.WorkflowScheduleSummary;
import org.carl.infra.workflow.schedule.WorkflowScheduler;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Temporal Schedule implementation of the runtime-independent {@link WorkflowScheduler} API. */
public final class TemporalWorkflowScheduler implements WorkflowScheduler {

    private final ScheduleClient client;
    private final String taskQueue;

    /**
     * Creates an adapter sharing the workflow client's connection, namespace, converter, identity,
     * and context propagators.
     */
    public TemporalWorkflowScheduler(WorkflowClient workflowClient, String taskQueue) {
        Objects.requireNonNull(workflowClient, "workflowClient");
        this.taskQueue = requireText(taskQueue, "taskQueue");
        ScheduleClientOptions options =
                ScheduleClientOptions.newBuilder()
                        .setNamespace(workflowClient.getOptions().getNamespace())
                        .setDataConverter(workflowClient.getOptions().getDataConverter())
                        .setIdentity(workflowClient.getOptions().getIdentity())
                        .setContextPropagators(workflowClient.getOptions().getContextPropagators())
                        .build();
        this.client =
                ScheduleClient.newInstance(workflowClient.getWorkflowServiceStubs(), options);
    }

    /** Constructor for integrations that already own a configured Temporal Schedule client. */
    public TemporalWorkflowScheduler(ScheduleClient client, String taskQueue) {
        this.client = Objects.requireNonNull(client, "client");
        this.taskQueue = requireText(taskQueue, "taskQueue");
    }

    @Override
    public WorkflowScheduleDescription create(
            WorkflowSchedule schedule, ScheduleCreateOptions createOptions) {
        Objects.requireNonNull(schedule, "schedule");
        Objects.requireNonNull(createOptions, "createOptions");
        try {
            ScheduleOptions options =
                    ScheduleOptions.newBuilder()
                            .setTriggerImmediately(createOptions.triggerImmediately())
                            .setBackfills(
                                    createOptions.backfills().stream()
                                            .map(TemporalScheduleMapper::toTemporal)
                                            .toList())
                            .build();
            client.createSchedule(
                    schedule.id(), TemporalScheduleMapper.toTemporal(schedule, taskQueue), options);
            return describe(schedule.id());
        } catch (ScheduleAlreadyRunningException e) {
            throw new WorkflowScheduleAlreadyExistsException(schedule.id(), e);
        } catch (RuntimeException e) {
            throw translate(schedule.id(), "create", e);
        }
    }

    @Override
    public WorkflowScheduleDescription describe(String scheduleId) {
        return execute(
                scheduleId,
                "describe",
                () -> TemporalScheduleMapper.fromTemporal(handle(scheduleId).describe()));
    }

    @Override
    public List<WorkflowScheduleSummary> list() {
        try {
            return client.listSchedules().map(TemporalScheduleMapper::fromTemporal).toList();
        } catch (RuntimeException e) {
            throw translate("<list>", "list", e);
        }
    }

    @Override
    public WorkflowScheduleDescription update(WorkflowSchedule schedule) {
        Objects.requireNonNull(schedule, "schedule");
        execute(
                schedule.id(),
                "update",
                () ->
                        handle(schedule.id())
                                .update(
                                        ignored ->
                                                new ScheduleUpdate(
                                                        TemporalScheduleMapper.toTemporal(
                                                                schedule, taskQueue))));
        return describe(schedule.id());
    }

    @Override
    public void pause(String scheduleId, String note) {
        execute(
                scheduleId,
                "pause",
                () -> {
                    if (note == null) {
                        handle(scheduleId).pause();
                    } else {
                        handle(scheduleId).pause(note);
                    }
                });
    }

    @Override
    public void resume(String scheduleId, String note) {
        execute(
                scheduleId,
                "resume",
                () -> {
                    if (note == null) {
                        handle(scheduleId).unpause();
                    } else {
                        handle(scheduleId).unpause(note);
                    }
                });
    }

    @Override
    public void trigger(String scheduleId) {
        execute(scheduleId, "trigger", () -> handle(scheduleId).trigger());
    }

    @Override
    public void trigger(String scheduleId, ScheduleOverlapPolicy overlapPolicy) {
        Objects.requireNonNull(overlapPolicy, "overlapPolicy");
        execute(
                scheduleId,
                "trigger",
                () -> handle(scheduleId).trigger(TemporalScheduleMapper.toTemporal(overlapPolicy)));
    }

    @Override
    public void backfill(String scheduleId, List<ScheduleBackfill> backfills) {
        Objects.requireNonNull(backfills, "backfills");
        if (backfills.isEmpty()) {
            throw new IllegalArgumentException("backfills must not be empty");
        }
        execute(
                scheduleId,
                "backfill",
                () ->
                        handle(scheduleId)
                                .backfill(
                                        backfills.stream()
                                                .map(TemporalScheduleMapper::toTemporal)
                                                .toList()));
    }

    @Override
    public void delete(String scheduleId) {
        execute(scheduleId, "delete", () -> handle(scheduleId).delete());
    }

    private ScheduleHandle handle(String scheduleId) {
        return client.getHandle(requireText(scheduleId, "scheduleId"));
    }

    private <T> T execute(String scheduleId, String operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            throw translate(scheduleId, operation, e);
        }
    }

    private void execute(String scheduleId, String operation, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            throw translate(scheduleId, operation, e);
        }
    }

    private RuntimeException translate(String scheduleId, String operation, RuntimeException error) {
        if (error instanceof WorkflowScheduleException workflowScheduleException) {
            return workflowScheduleException;
        }
        StatusRuntimeException statusError = findStatusError(error);
        if (statusError != null && statusError.getStatus().getCode() == Status.Code.NOT_FOUND) {
            return new WorkflowScheduleNotFoundException(scheduleId, error);
        }
        return new WorkflowScheduleException(
                "failed to " + operation + " workflow schedule '" + scheduleId + "'", error);
    }

    private static StatusRuntimeException findStatusError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof StatusRuntimeException statusError) {
                return statusError;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
