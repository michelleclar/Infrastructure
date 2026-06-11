package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.archive.ArchiveActivities;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.graph.GraphValidator;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.handlers.RuntimeIntents;
import org.carl.infrastructure.workflow.handlers.TaskGroupContract;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Temporal adapter for the workflow engine.
 *
 * <p>All orchestration logic lives in the platform-independent {@link WorkflowInterpreter}; this
 * class is the Temporal boundary: it implements the {@link GenericWorkflow} interface
 * ({@code @WorkflowMethod}/{@code @SignalMethod}/{@code @QueryMethod}) and the {@link RuntimeOps}
 * side-effect port (activity invocation, timers, signal await, taskGroup fan-out, child workflows,
 * async-hook dispatch, archival). {@code execute} builds the shared {@link ExecutionContext} and
 * delegates to {@code interpreter.run(...)}.
 *
 * <p>Signals are buffered into {@link #signalQueue} (with {@code eventId} de-duplication) by the
 * {@code @SignalMethod}; the interpreter consumes them through {@link #awaitEvent} using a pure
 * {@link RuntimeOps.EventMatcher}. The interpreter shares this instance's {@link ExecutionContext},
 * so {@link #runTaskGroup} records child results against the same state.
 */
public final class GenericWorkflowImpl implements GenericWorkflow, RuntimeOps {

    /** Default activity options: bounded start-to-close timeout, no app retries. */
    private static final ActivityOptions DEFAULT_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    /** Async interceptor hook options: short timeout, no retry (best-effort). */
    private static final ActivityOptions INTERCEPTOR_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    /** Archive options: short timeout, no retry; best-effort. */
    private static final ActivityOptions ARCHIVE_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    // Adapter-owned state ----------------------------------------------------------------------

    private final Deque<WorkflowEvent> signalQueue = new ArrayDeque<>();
    /**
     * Idempotency keys of signals already accepted. Workflow state: Temporal replays {@link #signal}
     * deterministically from history, so the set is rebuilt identically on replay. Only add/contains
     * are used (never iterated), so HashSet ordering does not affect determinism.
     */
    private final Set<String> processedEventIds = new HashSet<>();
    /** Async hook + archive promises, drained by {@link #awaitAsyncHooks()} before terminal return. */
    private final List<Promise<Void>> asyncHookPromises = new ArrayList<>();

    private ExecutionContext ctx;
    private WorkflowInterpreter interpreter;
    private Instant startedAt;
    private boolean archiveEnabled;

    // GenericWorkflow (Temporal entry points) --------------------------------------------------

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        NodeHandlerRegistry registry = HandlerHolder.registry();
        ObjectMapper mapper = ObjectMapperHolder.mapper();

        // Normalise DSL-emitted shapes (e.g. taskGroup's nested {"join":{"type":"all"}}) into the
        // wire form expected by the handler config records.
        WorkflowDefinition definition =
                NodeConfigCodec.normalizeDefinition(input.workflowDefinition());

        GraphValidator.validate(definition, registry).throwIfInvalid();
        WorkflowGraph graph = new WorkflowGraph(definition);

        this.ctx =
                new ExecutionContext(
                        Workflow.getInfo().getWorkflowId(),
                        Workflow.getInfo().getRunId(),
                        definition.id(),
                        input.businessData(),
                        input.initialVariables());
        this.startedAt = Instant.now();
        this.archiveEnabled = input.archiveEnabled();
        this.interpreter =
                new WorkflowInterpreter(
                        definition, graph, registry, mapper, ctx, this, InterceptorHolder.registry());

        return interpreter.run(input.startNodeId());
    }

    @Override
    public void signal(WorkflowEvent event) {
        if (event == null) {
            return;
        }
        String id = event.eventId();
        if (id != null && !id.isBlank() && !processedEventIds.add(id)) {
            // Duplicate signal: same eventId already accepted — drop for idempotency.
            return;
        }
        signalQueue.add(event);
    }

    @Override
    public WorkflowState query() {
        WorkflowInterpreter i = interpreter;
        if (i == null) {
            return new WorkflowState(null, false, Map.of(), null);
        }
        return i.snapshotState();
    }

    // RuntimeOps (Temporal side-effect implementations) ----------------------------------------

    @Override
    public ActivityResult runActivity(String activityName, Map<String, Object> input) {
        GenericActivity stub =
                Workflow.newActivityStub(GenericActivity.class, DEFAULT_ACTIVITY_OPTIONS);
        return stub.execute(new ActivityCall(activityName, input));
    }

    @Override
    public void sleep(Duration duration) {
        Workflow.sleep(duration);
    }

    @Override
    public AwaitOutcome awaitEvent(EventMatcher matcher, Duration timeout) {
        if (timeout == null) {
            Workflow.await(() -> anyMatch(matcher));
            return new AwaitOutcome(pollMatch(matcher), false);
        }
        boolean got = Workflow.await(timeout, () -> anyMatch(matcher));
        if (!got) {
            return new AwaitOutcome(null, true);
        }
        return new AwaitOutcome(pollMatch(matcher), false);
    }

    @Override
    public WorkflowResult runSubProcess(
            WorkflowDefinition childDef, Map<String, Object> input, String idHint) {
        String childWorkflowId = Workflow.getInfo().getWorkflowId() + "/sub/" + idHint;
        GenericWorkflow childStub =
                Workflow.newChildWorkflowStub(
                        GenericWorkflow.class,
                        ChildWorkflowOptions.newBuilder().setWorkflowId(childWorkflowId).build());
        return childStub.execute(WorkflowInput.from(childDef, input));
    }

    @Override
    public NodeResult runTaskGroup(
            Map<String, Object> intentPayload,
            NodeDefinition parentNode,
            String parentQualifier,
            TaskGroupChildExecutor childExecutor,
            TaskGroupJoiner joiner) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children =
                (List<Map<String, Object>>) intentPayload.get(RuntimeIntents.CHILDREN);
        if (children == null || children.isEmpty()) {
            return joiner.onChildCompleted();
        }

        ObjectMapper mapper = ObjectMapperHolder.mapper();

        // Fan-out: each child runs in its own coroutine so they can independently consume signals.
        // Wrapped in a CancellationScope for short-circuit (ANY: first APPROVED wins; ALL: first
        // REJECTED loses).
        List<Promise<NodeResult>> promises = new ArrayList<>(children.size());
        List<String> childIds = new ArrayList<>(children.size());
        boolean[] consumed = new boolean[children.size()];
        for (Map<String, Object> childSpec : children) {
            childIds.add((String) childSpec.get("id"));
        }
        CancellationScope scope =
                Workflow.newCancellationScope(
                        () -> {
                            for (int i = 0; i < children.size(); i++) {
                                Map<String, Object> childSpec = children.get(i);
                                String childId = childIds.get(i);
                                String childType = (String) childSpec.get("type");
                                String childLabel = (String) childSpec.get("label");
                                Object rawConfig = childSpec.get("config");
                                JsonNode childConfig =
                                        rawConfig == null ? null : mapper.valueToTree(rawConfig);
                                NodeDefinition childNode =
                                        new NodeDefinition(
                                                childId, childLabel, childType, null, childConfig);
                                String qualifier =
                                        TaskGroupContract.childKey(parentQualifier, childId);
                                promises.add(
                                        Async.function(
                                                () ->
                                                        executeChildSafely(
                                                                childNode, qualifier, childExecutor)));
                            }
                        });
        scope.run();

        // Join loop: wait for any pending child, record its result, let the interpreter fold it into
        // the taskGroup aggregate; on short-circuit cancel the scope and drain the rest.
        NodeResult aggregated = NodeResult.waiting();
        int remaining = children.size();
        while (remaining > 0) {
            List<Promise<NodeResult>> pending = new ArrayList<>();
            for (int i = 0; i < promises.size(); i++) {
                if (!consumed[i]) {
                    pending.add(promises.get(i));
                }
            }
            if (pending.isEmpty()) {
                break;
            }
            Promise.anyOf(pending).get();

            for (int i = 0; i < promises.size(); i++) {
                if (consumed[i]) continue;
                Promise<NodeResult> p = promises.get(i);
                if (!p.isCompleted()) continue;
                NodeResult r = p.get();
                ctx.recordResult(TaskGroupContract.childKey(parentQualifier, childIds.get(i)), r);
                consumed[i] = true;
                remaining--;

                ctx.setCurrentNodeId(parentQualifier);
                aggregated = joiner.onChildCompleted();
                if (aggregated.status() != NodeStatus.WAITING) {
                    scope.cancel(
                            "taskGroup '"
                                    + parentQualifier
                                    + "' short-circuited with outcome "
                                    + aggregated.outcome());
                    for (int j = 0; j < promises.size(); j++) {
                        if (consumed[j]) continue;
                        NodeResult late;
                        try {
                            late = promises.get(j).get();
                        } catch (CanceledFailure cf) {
                            late = NodeResult.cancelled();
                        }
                        ctx.recordResult(
                                TaskGroupContract.childKey(parentQualifier, childIds.get(j)), late);
                        consumed[j] = true;
                    }
                    return aggregated;
                }
            }
        }
        return aggregated;
    }

    @Override
    public void emitAsyncHook(AsyncHookInvocation invocation) {
        AsyncInterceptorActivity stub =
                Workflow.newActivityStub(AsyncInterceptorActivity.class, INTERCEPTOR_ACTIVITY_OPTIONS);
        asyncHookPromises.add(Async.procedure(stub::invoke, invocation));
    }

    @Override
    public void awaitAsyncHooks() {
        if (asyncHookPromises.isEmpty()) {
            return;
        }
        try {
            Promise.allOf(asyncHookPromises).get();
        } catch (RuntimeException ignored) {
            Workflow.getLogger(GenericWorkflowImpl.class)
                    .warn("One or more async interceptor hooks failed; workflow result unaffected");
        }
    }

    @Override
    public void archive(WorkflowResult result) {
        if (!archiveEnabled) {
            return;
        }
        if (result == null || result.finalStatus() == null) {
            return;
        }
        WorkflowInstanceSnapshot snapshot =
                new WorkflowInstanceSnapshot(
                        Workflow.getInfo().getWorkflowId(),
                        Workflow.getInfo().getRunId(),
                        ctx.definitionId(),
                        null,
                        result.finalStatus().name(),
                        startedAt,
                        Instant.now(),
                        result.finalNodeId(),
                        result.finalOutcome(),
                        result.finalStatus().name(),
                        ctx.businessData(),
                        result.finalVariables(),
                        result.executionRecords());
        ArchiveActivities archiveStub =
                Workflow.newActivityStub(ArchiveActivities.class, ARCHIVE_ACTIVITY_OPTIONS);
        asyncHookPromises.add(Async.procedure(archiveStub::archive, snapshot));
    }

    @Override
    public void warn(String message) {
        Workflow.getLogger(GenericWorkflowImpl.class).warn(message);
    }

    // Signal matching (over the adapter-owned queue) -------------------------------------------

    private boolean anyMatch(EventMatcher matcher) {
        for (WorkflowEvent ev : signalQueue) {
            if (matcher.matches(ev)) {
                return true;
            }
        }
        return false;
    }

    private WorkflowEvent pollMatch(EventMatcher matcher) {
        Iterator<WorkflowEvent> it = signalQueue.iterator();
        while (it.hasNext()) {
            WorkflowEvent ev = it.next();
            if (matcher.matches(ev)) {
                it.remove();
                return ev;
            }
        }
        return null;
    }

    /** Run a taskGroup child via the interpreter callback, mapping Temporal cancellation. */
    private NodeResult executeChildSafely(
            NodeDefinition childNode, String qualifier, TaskGroupChildExecutor childExecutor) {
        try {
            return childExecutor.execute(childNode, qualifier);
        } catch (CanceledFailure cf) {
            return NodeResult.cancelled();
        } catch (RuntimeException re) {
            return NodeResult.failed(
                    "child '"
                            + qualifier
                            + "' threw: "
                            + re.getClass().getSimpleName()
                            + ": "
                            + re.getMessage());
        }
    }
}
