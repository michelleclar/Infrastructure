package org.carl.infra.workflow.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infra.workflow.definition.NodeDefinition;
import org.carl.infra.workflow.definition.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WorkflowScheduleContractTest {

    @Test
    void convenienceFactoriesCreateExplicitDefaults() {
        ScheduleSpec spec = ScheduleSpec.cron("0 2 * * *", "Asia/Shanghai");
        ScheduledWorkflowAction action =
                ScheduledWorkflowAction.of("daily-report", definition(), Map.of("tenant", "a"));

        WorkflowSchedule schedule = WorkflowSchedule.of("daily-report", action, spec);

        assertEquals(List.of("0 2 * * *"), schedule.spec().cronExpressions());
        assertEquals("Asia/Shanghai", schedule.spec().timeZoneName());
        assertEquals(ScheduleOverlapPolicy.SKIP, schedule.policy().overlapPolicy());
        assertFalse(schedule.state().paused());
        assertEquals(Map.of(), schedule.action().initialVariables());
        assertFalse(schedule.action().archive());
    }

    @Test
    void definitionModelsDefensivelyCopyCollections() {
        List<String> cron = new ArrayList<>(List.of("0 * * * *"));
        Map<String, Object> data = new HashMap<>(Map.of("key", "value"));
        data.put("nullable", null);

        ScheduleSpec spec =
                new ScheduleSpec(
                        List.of(), List.of(), cron, List.of(), null, null, null, null);
        ScheduledWorkflowAction action =
                ScheduledWorkflowAction.of("hourly", definition(), data);

        cron.add("30 * * * *");
        data.put("other", "changed");

        assertEquals(List.of("0 * * * *"), spec.cronExpressions());
        assertEquals("value", action.businessData().get("key"));
        assertTrue(action.businessData().containsKey("nullable"));
        assertEquals(null, action.businessData().get("nullable"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> action.businessData().put("x", "y"));
    }

    @Test
    void scheduleSpecSupportsCalendarIntervalAndExclusions() {
        ScheduleSpec.Calendar weekdaysAtNine =
                new ScheduleSpec.Calendar(
                        null,
                        List.of(new ScheduleSpec.Range(30)),
                        List.of(new ScheduleSpec.Range(9)),
                        null,
                        null,
                        null,
                        List.of(new ScheduleSpec.Range(1, 5)),
                        "weekdays");
        ScheduleSpec.Calendar firstDay =
                new ScheduleSpec.Calendar(
                        null,
                        null,
                        null,
                        List.of(new ScheduleSpec.Range(1)),
                        null,
                        null,
                        null,
                        "skip first day");

        ScheduleSpec spec =
                new ScheduleSpec(
                        List.of(weekdaysAtNine),
                        List.of(new ScheduleSpec.Interval(Duration.ofHours(12), Duration.ofHours(1))),
                        List.of(),
                        List.of(firstDay),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-12-31T23:59:59Z"),
                        Duration.ofMinutes(5),
                        "Asia/Shanghai");

        assertEquals(30, spec.calendars().getFirst().minutes().getFirst().start());
        assertEquals(1, spec.skipCalendars().getFirst().daysOfMonth().getFirst().start());
        assertEquals(Duration.ofHours(12), spec.intervals().getFirst().every());
    }

    @Test
    void rejectsInvalidScheduleInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ScheduleSpec(
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                null,
                                null,
                                null,
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScheduleSpec.Interval(Duration.ZERO, null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ScheduleSpec.Calendar(
                                List.of(new ScheduleSpec.Range(60)),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ScheduledWorkflowAction(
                                "workflow",
                                definition(),
                                Map.of(),
                                Map.of(),
                                "missing",
                                false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ScheduleState(null, false, -1L));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ScheduleBackfill(
                                Instant.parse("2026-01-02T00:00:00Z"),
                                Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Test
    void limitedAndPausedStateRemainExplicit() {
        ScheduleState state = new ScheduleState("maintenance", true, 3L);
        SchedulePolicy policy =
                new SchedulePolicy(
                        ScheduleOverlapPolicy.BUFFER_ONE, Duration.ofHours(2), true);

        assertTrue(state.paused());
        assertEquals(3L, state.remainingActions());
        assertTrue(policy.pauseOnFailure());
        assertEquals(Duration.ofHours(2), policy.catchupWindow());
    }

    private static WorkflowDefinition definition() {
        NodeDefinition end = new NodeDefinition("end", "End", "endTask", null, null);
        return new WorkflowDefinition("scheduled-flow", "Scheduled flow", List.of(end), List.of(), "end");
    }
}
