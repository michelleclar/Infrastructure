package org.carl.infra.workflow.schedule.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import org.carl.infra.workflow.definition.NodeDefinition;
import org.carl.infra.workflow.definition.WorkflowDefinition;
import org.carl.infra.workflow.dsl.BuiltInNodes;
import org.carl.infra.workflow.dsl.Flow;
import org.carl.infra.workflow.dsl.FlowDef;
import org.carl.infra.workflow.handlers.BuiltInHandlers;
import org.carl.infra.workflow.runtime.BusinessActivityRegistry;
import org.carl.infra.workflow.runtime.GenericWorkflow;
import org.carl.infra.workflow.runtime.WorkerSetup;
import org.carl.infra.workflow.runtime.WorkflowResult;
import org.carl.infra.workflow.schedule.ScheduleBackfill;
import org.carl.infra.workflow.schedule.ScheduleCreateOptions;
import org.carl.infra.workflow.schedule.ScheduleInfo;
import org.carl.infra.workflow.schedule.ScheduleOverlapPolicy;
import org.carl.infra.workflow.schedule.SchedulePolicy;
import org.carl.infra.workflow.schedule.ScheduleSpec;
import org.carl.infra.workflow.schedule.ScheduleState;
import org.carl.infra.workflow.schedule.ScheduledWorkflowAction;
import org.carl.infra.workflow.schedule.WorkflowSchedule;
import org.carl.infra.workflow.schedule.WorkflowScheduleAlreadyExistsException;
import org.carl.infra.workflow.schedule.WorkflowScheduleDescription;
import org.carl.infra.workflow.schedule.WorkflowScheduleException;
import org.carl.infra.workflow.schedule.WorkflowScheduleNotFoundException;
import org.carl.infra.workflow.spi.NodeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Full Schedule API tests against a real Temporal Server.
 *
 * <p>Run against the isolated Docker development server with:
 *
 * <pre>{@code
 * TEMPORAL_SCHEDULE_TEST_TARGET=127.0.0.1:17233 \
 * ./gradlew :infra-component-workflow-temporal:test \
 *   --tests org.carl.infra.workflow.schedule.temporal.TemporalWorkflowSchedulerServerTest
 * }</pre>
 */
@EnabledIfEnvironmentVariable(named = "TEMPORAL_SCHEDULE_TEST_TARGET", matches = ".+")
class TemporalWorkflowSchedulerServerTest {

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);

    private final Set<String> scheduleIds = new LinkedHashSet<>();

    private WorkflowServiceStubs service;
    private WorkflowClient client;
    private WorkerFactory workerFactory;
    private TemporalWorkflowScheduler scheduler;
    private String taskQueue;

    @BeforeEach
    void setUp() {
        String target = System.getenv("TEMPORAL_SCHEDULE_TEST_TARGET");
        taskQueue = "SCHEDULE_SERVER_TEST_" + UUID.randomUUID();
        service =
                WorkflowServiceStubs.newInstance(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
        client =
                WorkflowClient.newInstance(
                        service,
                        WorkflowClientOptions.newBuilder().setNamespace("default").build());
        workerFactory = WorkerFactory.newInstance(client);
        Worker worker = workerFactory.newWorker(taskQueue);
        NodeHandlerRegistry handlers = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlers);
        WorkerSetup.setup(worker, handlers, new BusinessActivityRegistry());
        workerFactory.start();
        scheduler = new TemporalWorkflowScheduler(client, taskQueue);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            for (String scheduleId : scheduleIds) {
                try {
                    scheduler.delete(scheduleId);
                } catch (WorkflowScheduleException ignored) {
                    // The test may already have deleted the schedule.
                }
            }
        }
        if (workerFactory != null) {
            workerFactory.shutdown();
        }
        if (service != null) {
            service.shutdown();
        }
    }

    @Test
    @Timeout(60)
    void completeManagementLifecycleRunsScheduledWorkflows() throws Exception {
        String scheduleId = track("lifecycle");
        WorkflowSchedule initial =
                schedule(
                        scheduleId,
                        completedFlow(),
                        ScheduleSpec.interval(Duration.ofHours(1)),
                        ScheduleOverlapPolicy.SKIP,
                        new ScheduleState("created paused", true, null));

        WorkflowScheduleDescription created = scheduler.create(initial);
        assertEquals(scheduleId, created.schedule().id());
        assertEquals("scheduled-flow", created.schedule().action().definition().id());
        assertEquals("server-test", created.schedule().action().businessData().get("source"));
        assertTrue(created.schedule().state().paused());
        assertEquals(Duration.ofHours(1), created.schedule().spec().intervals().getFirst().every());
        assertThrows(WorkflowScheduleAlreadyExistsException.class, () -> scheduler.create(initial));

        awaitListContains(scheduleId);
        scheduler.resume(scheduleId, "resumed");
        assertFalse(awaitDescription(scheduleId, value -> !value.schedule().state().paused())
                .schedule()
                .state()
                .paused());
        scheduler.pause(scheduleId, "paused for deterministic test");
        assertTrue(awaitDescription(scheduleId, value -> value.schedule().state().paused())
                .schedule()
                .state()
                .paused());

        WorkflowSchedule updated =
                schedule(
                        scheduleId,
                        completedFlow(),
                        ScheduleSpec.interval(Duration.ofHours(1)),
                        ScheduleOverlapPolicy.ALLOW_ALL,
                        new ScheduleState("updated", true, null));
        WorkflowScheduleDescription updateResult = scheduler.update(updated);
        assertEquals(
                Duration.ofHours(1),
                updateResult.schedule().spec().intervals().getFirst().every());
        assertEquals(
                ScheduleOverlapPolicy.ALLOW_ALL,
                updateResult.schedule().policy().overlapPolicy());

        long actionCount = updateResult.info().actionCount();
        for (ScheduleOverlapPolicy overlapPolicy : ScheduleOverlapPolicy.values()) {
            scheduler.trigger(scheduleId, overlapPolicy);
            long expectedActionCount = actionCount + 1;
            WorkflowScheduleDescription triggered =
                    awaitDescription(
                            scheduleId,
                            value -> value.info().actionCount() >= expectedActionCount);
            actionCount = triggered.info().actionCount();
            ScheduleInfo.ActionResult action = triggered.info().recentActions().getLast();
            WorkflowResult result = awaitResult(action.execution());
            assertEquals("end", result.finalNodeId());
        }

        Instant backfillEnd = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant backfillStart = backfillEnd.minus(2, ChronoUnit.HOURS).plusSeconds(1);
        long beforeBackfill = actionCount;
        scheduler.backfill(
                scheduleId,
                List.of(
                        new ScheduleBackfill(
                                backfillStart,
                                backfillEnd,
                                ScheduleOverlapPolicy.ALLOW_ALL)));
        WorkflowScheduleDescription afterBackfill =
                awaitDescription(
                        scheduleId,
                        value -> value.info().actionCount() >= beforeBackfill + 2);
        assertEquals(beforeBackfill + 2, afterBackfill.info().actionCount());

        scheduler.delete(scheduleId);
        scheduleIds.remove(scheduleId);
        assertThrows(WorkflowScheduleNotFoundException.class, () -> scheduler.describe(scheduleId));
    }

    @Test
    @Timeout(45)
    void createOptionsApplyImmediateTriggerAndHistoricalBackfill() throws Exception {
        String scheduleId = track("create-options");
        WorkflowSchedule schedule =
                schedule(
                        scheduleId,
                        completedFlow(),
                        ScheduleSpec.interval(Duration.ofHours(1)),
                        ScheduleOverlapPolicy.ALLOW_ALL,
                        new ScheduleState("manual only", true, null));
        Instant end = Instant.now().truncatedTo(ChronoUnit.HOURS);
        Instant start = end.minus(2, ChronoUnit.HOURS).plusSeconds(1);

        scheduler.create(
                schedule,
                new ScheduleCreateOptions(
                        true,
                        List.of(
                                new ScheduleBackfill(
                                        start, end, ScheduleOverlapPolicy.ALLOW_ALL))));

        WorkflowScheduleDescription description =
                awaitDescription(scheduleId, value -> value.info().actionCount() >= 3);
        assertEquals(3, description.info().actionCount());
        assertFalse(description.info().recentActions().isEmpty());
        for (ScheduleInfo.ActionResult action : description.info().recentActions()) {
            assertEquals("end", awaitResult(action.execution()).finalNodeId());
        }
    }

    @Test
    @Timeout(60)
    void overlapPoliciesControlConcurrentWorkflowExecutions() throws Exception {
        String scheduleId = track("overlap");
        WorkflowSchedule skipSchedule =
                schedule(
                        scheduleId,
                        approvalFlow(),
                        ScheduleSpec.interval(Duration.ofDays(1)),
                        ScheduleOverlapPolicy.SKIP,
                        new ScheduleState("manual triggers", true, null));
        scheduler.create(skipSchedule);

        scheduler.trigger(scheduleId);
        WorkflowScheduleDescription firstRunning =
                awaitDescription(scheduleId, value -> value.info().runningActions().size() == 1);
        ScheduleInfo.Execution firstExecution = firstRunning.info().runningActions().getFirst();

        scheduler.trigger(scheduleId);
        WorkflowScheduleDescription skipped =
                awaitDescription(
                        scheduleId, value -> value.info().skippedOverlapCount() >= 1);
        assertEquals(1, skipped.info().actionCount());
        approve(firstExecution);
        awaitDescription(scheduleId, value -> value.info().runningActions().isEmpty());

        WorkflowSchedule allowAllSchedule =
                schedule(
                        scheduleId,
                        approvalFlow(),
                        ScheduleSpec.interval(Duration.ofDays(1)),
                        ScheduleOverlapPolicy.ALLOW_ALL,
                        new ScheduleState("manual triggers", true, null));
        scheduler.update(allowAllSchedule);

        long beforeConcurrent = scheduler.describe(scheduleId).info().actionCount();
        scheduler.trigger(scheduleId, ScheduleOverlapPolicy.ALLOW_ALL);
        WorkflowScheduleDescription firstAllowed =
                awaitDescription(
                        scheduleId,
                        value -> value.info().actionCount() >= beforeConcurrent + 1);
        ScheduleInfo.ActionResult firstAllowedAction = firstAllowed.info().recentActions().getLast();
        awaitAfterScheduledSecond(firstAllowedAction.scheduledAt());
        scheduler.trigger(scheduleId, ScheduleOverlapPolicy.ALLOW_ALL);
        WorkflowScheduleDescription concurrent =
                awaitDescription(
                        scheduleId,
                        value -> value.info().actionCount() >= beforeConcurrent + 2);
        assertEquals(beforeConcurrent + 2, concurrent.info().actionCount());
        ScheduleInfo.Execution firstAllowedExecution = firstAllowedAction.execution();
        ScheduleInfo.Execution secondAllowedExecution =
                concurrent.info().recentActions().getLast().execution();
        assertNotEquals(firstAllowedExecution.workflowId(), secondAllowedExecution.workflowId());
        assertRunning(firstAllowedExecution);
        assertRunning(secondAllowedExecution);
        approve(firstAllowedExecution);
        approve(secondAllowedExecution);
    }

    private WorkflowSchedule schedule(
            String scheduleId,
            WorkflowDefinition definition,
            ScheduleSpec spec,
            ScheduleOverlapPolicy overlapPolicy,
            ScheduleState state) {
        return new WorkflowSchedule(
                scheduleId,
                new ScheduledWorkflowAction(
                        "workflow-" + scheduleId,
                        definition,
                        Map.of("source", "server-test"),
                        Map.of("initial", true),
                        definition.startNodeId(),
                        false),
                spec,
                new SchedulePolicy(overlapPolicy, Duration.ofHours(24), false),
                state);
    }

    private String track(String prefix) {
        String scheduleId = prefix + "-" + UUID.randomUUID();
        scheduleIds.add(scheduleId);
        return scheduleId;
    }

    private void awaitListContains(String scheduleId) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (scheduler.list().stream().anyMatch(value -> scheduleId.equals(value.id()))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("schedule did not appear in list: " + scheduleId);
    }

    private static void awaitAfterScheduledSecond(Instant scheduledAt) throws InterruptedException {
        Instant boundary = scheduledAt.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        while (!Instant.now().isAfter(boundary)) {
            Thread.sleep(20);
        }
    }

    private WorkflowScheduleDescription awaitDescription(
            String scheduleId, Predicate<WorkflowScheduleDescription> condition)
            throws InterruptedException {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        WorkflowScheduleDescription last = scheduler.describe(scheduleId);
        while (System.nanoTime() < deadline) {
            if (condition.test(last)) {
                return last;
            }
            Thread.sleep(100);
            last = scheduler.describe(scheduleId);
        }
        throw new AssertionError("schedule condition timed out: " + last);
    }

    private WorkflowResult awaitResult(ScheduleInfo.Execution execution)
            throws TimeoutException {
        GenericWorkflow workflow = workflow(execution);
        return WorkflowStub.fromTyped(workflow)
                .getResult(20, TimeUnit.SECONDS, WorkflowResult.class);
    }

    private void assertRunning(ScheduleInfo.Execution execution) {
        assertEquals(
                WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
                WorkflowStub.fromTyped(workflow(execution)).describe().getStatus());
    }

    private void approve(ScheduleInfo.Execution execution) throws TimeoutException {
        GenericWorkflow workflow = workflow(execution);
        workflow.signal(
                new org.carl.infra.workflow.spi.WorkflowEvent(
                        "approval",
                        JsonNodeFactory.instance.objectNode().put("decision", "approved")));
        WorkflowResult result =
                WorkflowStub.fromTyped(workflow)
                        .getResult(20, TimeUnit.SECONDS, WorkflowResult.class);
        assertEquals("end", result.finalNodeId());
    }

    private GenericWorkflow workflow(ScheduleInfo.Execution execution) {
        return client.newWorkflowStub(
                GenericWorkflow.class,
                execution.workflowId(),
                Optional.of(execution.firstExecutionRunId()));
    }

    private static WorkflowDefinition completedFlow() {
        NodeDefinition end = new NodeDefinition("end", "End", "endTask", null, null);
        return new WorkflowDefinition(
                "scheduled-flow", "Scheduled flow", List.of(end), List.of(), "end");
    }

    private static WorkflowDefinition approvalFlow() {
        FlowDef flow = Flow.define("scheduled-approval", "Scheduled approval");
        flow.start("approval");
        flow.node("approval", BuiltInNodes.approval("manager"));
        flow.node("end", builder -> builder.type("endTask").label("End"));
        flow.from("approval").on("APPROVED").to("end");
        return flow.build();
    }
}
