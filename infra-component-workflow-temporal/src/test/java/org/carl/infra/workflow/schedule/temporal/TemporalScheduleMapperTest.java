package org.carl.infra.workflow.schedule.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.temporal.client.schedules.ScheduleActionStartWorkflow;

import org.carl.infra.workflow.definition.NodeDefinition;
import org.carl.infra.workflow.definition.WorkflowDefinition;
import org.carl.infra.workflow.runtime.WorkflowInput;
import org.carl.infra.workflow.schedule.ScheduleOverlapPolicy;
import org.carl.infra.workflow.schedule.SchedulePolicy;
import org.carl.infra.workflow.schedule.ScheduleSpec;
import org.carl.infra.workflow.schedule.ScheduleState;
import org.carl.infra.workflow.schedule.ScheduledWorkflowAction;
import org.carl.infra.workflow.schedule.WorkflowSchedule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

class TemporalScheduleMapperTest {

    @Test
    void mapsCompleteCoreScheduleToTemporalActionAndBack() {
        ScheduleSpec.Calendar calendar =
                new ScheduleSpec.Calendar(
                        List.of(new ScheduleSpec.Range(0)),
                        List.of(new ScheduleSpec.Range(15, 45, 15)),
                        List.of(new ScheduleSpec.Range(9, 17)),
                        null,
                        null,
                        null,
                        List.of(new ScheduleSpec.Range(1, 5)),
                        "business hours");
        ScheduleSpec spec =
                new ScheduleSpec(
                        List.of(calendar),
                        List.of(new ScheduleSpec.Interval(Duration.ofHours(6), Duration.ofMinutes(5))),
                        List.of("0 2 * * *"),
                        List.of(
                                new ScheduleSpec.Calendar(
                                        null,
                                        null,
                                        null,
                                        List.of(new ScheduleSpec.Range(1)),
                                        null,
                                        null,
                                        null,
                                        "month start")),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-12-31T23:59:59Z"),
                        Duration.ofMinutes(3),
                        "Asia/Shanghai");
        WorkflowSchedule source =
                new WorkflowSchedule(
                        "billing",
                        new ScheduledWorkflowAction(
                                "billing-workflow",
                                definition(),
                                Map.of("tenant", "alpha"),
                                Map.of("attempt", 1),
                                "end",
                                true),
                        spec,
                        new SchedulePolicy(
                                ScheduleOverlapPolicy.BUFFER_ONE, Duration.ofHours(1), true),
                        new ScheduleState("initial pause", true, 5L));

        io.temporal.client.schedules.Schedule mapped =
                TemporalScheduleMapper.toTemporal(source, "BILLING_TASKS");
        ScheduleActionStartWorkflow action =
                (ScheduleActionStartWorkflow) mapped.getAction();
        WorkflowInput input = action.getArguments().get(0, WorkflowInput.class);

        assertEquals("billing-workflow", action.getOptions().getWorkflowId());
        assertEquals("BILLING_TASKS", action.getOptions().getTaskQueue());
        assertEquals("scheduled-flow", input.workflowDefinition().id());
        assertEquals("alpha", input.businessData().get("tenant").asText());
        assertEquals(1, input.initialVariables().get("attempt"));
        assertEquals("end", input.startNodeId());
        assertTrue(input.archiveEnabled());
        assertEquals(source.spec(), TemporalScheduleMapper.fromTemporal(mapped.getSpec()));
        assertEquals(source.policy(), TemporalScheduleMapper.fromTemporal(mapped.getPolicy()));
        assertEquals(source.state(), TemporalScheduleMapper.fromTemporal(mapped.getState()));
    }

    @Test
    void mapsEveryOverlapPolicyWithoutFallback() {
        for (ScheduleOverlapPolicy overlapPolicy : ScheduleOverlapPolicy.values()) {
            SchedulePolicy source = new SchedulePolicy(overlapPolicy, null, false);
            SchedulePolicy roundTrip =
                    TemporalScheduleMapper.fromTemporal(
                            TemporalScheduleMapper.toTemporal(source));
            assertEquals(source, roundTrip);
        }
    }

    @Test
    void unlimitedActiveStateRoundTrips() {
        ScheduleState roundTrip =
                TemporalScheduleMapper.fromTemporal(
                        TemporalScheduleMapper.toTemporal(ScheduleState.active()));

        assertFalse(roundTrip.paused());
        assertEquals(null, roundTrip.remainingActions());
    }

    private static WorkflowDefinition definition() {
        NodeDefinition end = new NodeDefinition("end", "End", "endTask", null, null);
        return new WorkflowDefinition("scheduled-flow", "Scheduled flow", List.of(end), List.of(), "end");
    }
}
