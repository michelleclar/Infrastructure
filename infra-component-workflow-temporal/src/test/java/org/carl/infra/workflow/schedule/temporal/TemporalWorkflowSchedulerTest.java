package org.carl.infra.workflow.schedule.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.Status;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleAlreadyRunningException;
import io.temporal.client.schedules.ScheduleBackfill;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleDescription;
import io.temporal.client.schedules.ScheduleException;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleInfo;
import io.temporal.client.schedules.ScheduleListActionStartWorkflow;
import io.temporal.client.schedules.ScheduleListDescription;
import io.temporal.client.schedules.ScheduleListInfo;
import io.temporal.client.schedules.ScheduleListSchedule;
import io.temporal.client.schedules.ScheduleListState;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.ScheduleState;
import io.temporal.client.schedules.ScheduleUpdate;
import io.temporal.client.schedules.ScheduleUpdateInput;
import io.temporal.common.SearchAttributes;
import io.temporal.common.converter.DataConverter;
import io.temporal.workflow.Functions;

import org.carl.infra.workflow.definition.NodeDefinition;
import org.carl.infra.workflow.definition.WorkflowDefinition;
import org.carl.infra.workflow.schedule.ScheduleCreateOptions;
import org.carl.infra.workflow.schedule.ScheduleSpec;
import org.carl.infra.workflow.schedule.ScheduledWorkflowAction;
import org.carl.infra.workflow.schedule.WorkflowSchedule;
import org.carl.infra.workflow.schedule.WorkflowScheduleAlreadyExistsException;
import org.carl.infra.workflow.schedule.WorkflowScheduleDescription;
import org.carl.infra.workflow.schedule.WorkflowScheduleNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class TemporalWorkflowSchedulerTest {

    @Test
    void implementsScheduleLifecycleWithoutLeakingTemporalTypes() {
        InMemoryScheduleClient client = new InMemoryScheduleClient();
        TemporalWorkflowScheduler scheduler =
                new TemporalWorkflowScheduler(client, "SCHEDULE_TEST_TASKS");
        WorkflowSchedule initial = schedule(ScheduleSpec.cron("0 2 * * *", "Asia/Shanghai"));

        WorkflowScheduleDescription created =
                scheduler.create(initial, new ScheduleCreateOptions(true, List.of()));

        assertEquals("billing", created.schedule().id());
        assertEquals("alpha", created.schedule().action().businessData().get("tenant"));
        assertTrue(client.triggeredOnCreate);
        assertThrows(
                WorkflowScheduleAlreadyExistsException.class,
                () -> scheduler.create(initial));

        scheduler.pause("billing", "maintenance");
        assertTrue(scheduler.describe("billing").schedule().state().paused());
        scheduler.resume("billing", "ready");
        assertFalse(scheduler.describe("billing").schedule().state().paused());

        WorkflowSchedule updated = schedule(ScheduleSpec.interval(Duration.ofHours(6)));
        WorkflowScheduleDescription updateResult = scheduler.update(updated);
        assertEquals(
                Duration.ofHours(6),
                updateResult.schedule().spec().intervals().getFirst().every());
        assertEquals(List.of("billing"), scheduler.list().stream().map(summary -> summary.id()).toList());

        scheduler.trigger("billing");
        scheduler.backfill(
                "billing",
                List.of(
                        new org.carl.infra.workflow.schedule.ScheduleBackfill(
                                Instant.parse("2026-01-01T00:00:00Z"),
                                Instant.parse("2026-01-02T00:00:00Z"))));
        assertEquals(1, client.triggerCount);
        assertEquals(1, client.backfillCount);

        scheduler.delete("billing");
        assertThrows(
                WorkflowScheduleNotFoundException.class,
                () -> scheduler.describe("billing"));
    }

    private static WorkflowSchedule schedule(ScheduleSpec spec) {
        NodeDefinition end = new NodeDefinition("end", "End", "endTask", null, null);
        WorkflowDefinition definition =
                new WorkflowDefinition(
                        "scheduled-flow", "Scheduled flow", List.of(end), List.of(), "end");
        return WorkflowSchedule.of(
                "billing",
                ScheduledWorkflowAction.of(
                        "billing-workflow", definition, Map.of("tenant", "alpha")),
                spec);
    }

    private static final class InMemoryScheduleClient implements ScheduleClient {
        private final Map<String, Schedule> schedules = new LinkedHashMap<>();
        private boolean triggeredOnCreate;
        private int triggerCount;
        private int backfillCount;

        @Override
        public ScheduleHandle createSchedule(
                String scheduleId, Schedule schedule, ScheduleOptions options) {
            if (schedules.containsKey(scheduleId)) {
                throw new ScheduleAlreadyRunningException(
                        Status.ALREADY_EXISTS.asRuntimeException());
            }
            schedules.put(scheduleId, schedule);
            triggeredOnCreate = options.isTriggerImmediately();
            backfillCount += options.getBackfills() == null ? 0 : options.getBackfills().size();
            return getHandle(scheduleId);
        }

        @Override
        public ScheduleHandle getHandle(String scheduleId) {
            return new InMemoryScheduleHandle(scheduleId);
        }

        @Override
        public Stream<ScheduleListDescription> listSchedules() {
            return listSchedules(null, null);
        }

        @Override
        public Stream<ScheduleListDescription> listSchedules(Integer pageSize) {
            return listSchedules(null, pageSize);
        }

        @Override
        public Stream<ScheduleListDescription> listSchedules(String query, Integer pageSize) {
            return schedules.entrySet().stream()
                    .map(
                            entry -> {
                                Schedule schedule = entry.getValue();
                                ScheduleState state = schedule.getState();
                                return new ScheduleListDescription(
                                        entry.getKey(),
                                        new ScheduleListSchedule(
                                                new ScheduleListActionStartWorkflow("GenericWorkflow"),
                                                schedule.getSpec(),
                                                new ScheduleListState(
                                                        state == null ? null : state.getNote(),
                                                        state != null && state.isPaused())),
                                        new ScheduleListInfo(List.of(), List.of()),
                                        Map.<String, Payload>of(),
                                        DataConverter.getDefaultInstance(),
                                        Map.of());
                            });
        }

        private Schedule requireSchedule(String scheduleId) {
            Schedule schedule = schedules.get(scheduleId);
            if (schedule == null) {
                throw new ScheduleException(Status.NOT_FOUND.asRuntimeException());
            }
            return schedule;
        }

        private ScheduleDescription description(String scheduleId) {
            Schedule schedule = requireSchedule(scheduleId);
            return new ScheduleDescription(
                    scheduleId,
                    new ScheduleInfo(
                            0,
                            0,
                            0,
                            List.of(),
                            List.of(),
                            List.of(),
                            Instant.parse("2026-01-01T00:00:00Z"),
                            null),
                    schedule,
                    Map.of(),
                    SearchAttributes.EMPTY,
                    Map.of(),
                    DataConverter.getDefaultInstance());
        }

        private final class InMemoryScheduleHandle implements ScheduleHandle {
            private final String scheduleId;

            private InMemoryScheduleHandle(String scheduleId) {
                this.scheduleId = scheduleId;
            }

            @Override
            public String getId() {
                return scheduleId;
            }

            @Override
            public void backfill(List<ScheduleBackfill> backfills) {
                requireSchedule(scheduleId);
                backfillCount += backfills.size();
            }

            @Override
            public void delete() {
                requireSchedule(scheduleId);
                schedules.remove(scheduleId);
            }

            @Override
            public ScheduleDescription describe() {
                return description(scheduleId);
            }

            @Override
            public void pause(String note) {
                changePaused(true, note);
            }

            @Override
            public void pause() {
                changePaused(true, null);
            }

            @Override
            public void trigger(ScheduleOverlapPolicy overlapPolicy) {
                requireSchedule(scheduleId);
                triggerCount++;
            }

            @Override
            public void trigger() {
                requireSchedule(scheduleId);
                triggerCount++;
            }

            @Override
            public void unpause(String note) {
                changePaused(false, note);
            }

            @Override
            public void unpause() {
                changePaused(false, null);
            }

            @Override
            public void update(Functions.Func1<ScheduleUpdateInput, ScheduleUpdate> updater) {
                ScheduleUpdate update = updater.apply(new ScheduleUpdateInput(description(scheduleId)));
                if (update != null) {
                    schedules.put(scheduleId, update.getSchedule());
                }
            }

            private void changePaused(boolean paused, String note) {
                Schedule current = requireSchedule(scheduleId);
                ScheduleState oldState = current.getState();
                ScheduleState state =
                        ScheduleState.newBuilder(oldState)
                                .setPaused(paused)
                                .setNote(note)
                                .build();
                schedules.put(
                        scheduleId,
                        Schedule.newBuilder(current).setState(state).build());
            }
        }
    }
}
