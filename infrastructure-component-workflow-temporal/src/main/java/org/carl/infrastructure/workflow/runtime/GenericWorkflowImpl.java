package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.CanceledFailure;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.archive.ArchiveActivities;
import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.interceptor.DeterministicInterceptor;
import org.carl.infrastructure.workflow.interceptor.InterceptorContext;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.engine.EdgeRouter;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.graph.GraphValidator;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.handlers.ApprovalTaskHandler;
import org.carl.infrastructure.workflow.handlers.EventTaskHandler;
import org.carl.infrastructure.workflow.handlers.RuntimeIntents;
import org.carl.infrastructure.workflow.handlers.ServiceTaskHandler;
import org.carl.infrastructure.workflow.handlers.SubProcessHandler;
import org.carl.infrastructure.workflow.handlers.TaskGroupContract;
import org.carl.infrastructure.workflow.handlers.TimerTaskHandler;
import org.carl.infrastructure.workflow.handlers.UserTaskHandler;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic Temporal workflow impl that interprets a {@link WorkflowDefinition} with the registered
 * {@link NodeHandler} set.
 *
 * <h2>Routing model</h2>
 *
 * After each node completes the runtime calls {@link #pickNextEdge}: outgoing edges are first
 * matched by {@link EdgeDefinition#event()} against the node's outcome name (this is the post-G2
 * primary path — the DSL {@code .on(name)} writes the outcome name into the {@code event} field).
 * The first edge whose {@link EdgeDefinition#when()} guard expression evaluates to {@code true}
 * (or is {@code null}/blank) wins. As a legacy fallback, edges whose {@link
 * EdgeDefinition#outcome()} equals the outcome are tried next (for hand-constructed definitions).
 * If neither matches, the runtime falls back to a single edge with no event/outcome/when (the
 * canonical "default" edge).
 *
 * <h2>WAITING translation</h2>
 *
 * Handlers never invoke side-effects directly. When {@code run()} or {@code onEvent()} returns
 * {@link NodeStatus#WAITING}, the runtime inspects the payload keys defined in {@link
 * RuntimeIntents} and performs the matching Temporal operation (activity invocation, timer sleep,
 * signal await, parallel sub-node fan-out, or child workflow). The synthetic completion event is
 * then fed back to the handler.
 *
 * <h2>Saga compensation</h2>
 *
 * Every node whose handler returns {@code compensable() == true} is pushed onto a compensation
 * stack when it completes successfully. On {@link NodeStatus#FAILED}, the stack is drained in
 * reverse order: each handler's {@link NodeHandler#compensate} is called and — if it returns a
 * WAITING+ACTIVITY intent — the compensating activity is executed before moving to the next
 * compensation step. Compensation is best-effort: a compensation activity that throws is logged and
 * skipped so all prior compensable nodes still get a chance to roll back.
 */
public final class GenericWorkflowImpl implements GenericWorkflow {

    /** Default activity options: bounded start-to-close timeout, no app retries. */
    private static final ActivityOptions DEFAULT_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    /**
     * Activity options for async interceptor hooks: short timeout, no retry (best-effort; errors
     * must not block the workflow).
     */
    private static final ActivityOptions INTERCEPTOR_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    // Mutable workflow state -----------------------------------------------------------------

    private final Deque<WorkflowEvent> signalQueue = new ArrayDeque<>();
    /**
     * Idempotency keys of signals already accepted. A signal whose {@link WorkflowEvent#eventId()}
     * is already present is dropped before it reaches the queue. This is workflow state: Temporal
     * replays {@link #signal} deterministically from history, so the set is rebuilt identically on
     * replay. Only add/contains are used (never iterated), so HashSet ordering does not affect
     * determinism.
     */
    private final Set<String> processedEventIds = new HashSet<>();
    private final List<CompensationRecord> compensationStack = new ArrayList<>();
    /**
     * Collected async hook promises. Populated by {@link #fireHook} when async interceptors are
     * registered; drained (waited on) before each {@code return result} in {@link #execute}.
     */
    private final List<Promise<Void>> asyncHookPromises = new ArrayList<>();
    private ExecutionContext ctx;
    private NodeHandlerRegistry registry;
    private ObjectMapper mapper;
    private boolean finished;
    private String lastEventName;
    private Instant startedAt;
    /** Per-execution archive opt-in flag, read from {@link WorkflowInput#archiveEnabled()}. */
    private boolean archiveEnabled;

    /** Tracks a successfully-completed compensable node for potential saga rollback. */
    private record CompensationRecord(
            String nodeId,
            NodeDefinition node,
            @SuppressWarnings("rawtypes") NodeHandler handler,
            Object config,
            NodeResult completedResult) {}

    // Workflow body --------------------------------------------------------------------------

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        this.registry = HandlerHolder.registry();
        this.mapper = ObjectMapperHolder.mapper();

        // Normalise DSL-emitted shapes (e.g. taskGroup's nested {"join":{"type":"all"}}) into the
        // wire form expected by the handler config records.
        WorkflowDefinition definition =
                NodeConfigCodec.normalizeDefinition(input.workflowDefinition());

        GraphValidator.validate(definition, registry).throwIfInvalid();
        WorkflowGraph graph = new WorkflowGraph(definition);

        String workflowId = Workflow.getInfo().getWorkflowId();
        String runId = Workflow.getInfo().getRunId();
        this.ctx =
                new ExecutionContext(
                        workflowId,
                        runId,
                        definition.id(),
                        input.businessData(),
                        input.initialVariables());
        this.startedAt = Instant.now();
        this.archiveEnabled = input.archiveEnabled();

        // ── WORKFLOW_START hook ──────────────────────────────────────────────────────────────
        fireHook(HookPhases.WORKFLOW_START, null, null, null, null);

        String startNodeId = EdgeRouter.resolveStartNode(input.startNodeId(), definition, graph);
        String currentNodeId = startNodeId;

        while (true) {
            NodeDefinition node = graph.node(currentNodeId);
            @SuppressWarnings("unchecked")
            NodeHandler<Object> handler = (NodeHandler<Object>) registry.lookup(node.type());
            Object config = NodeConfigCodec.decode(mapper, handler, node.type(), node.config());

            // ── NODE_ENTER hook ──────────────────────────────────────────────────────────────
            fireHook(HookPhases.NODE_ENTER, node, null, null, null);

            NodeResult lastResult = executeNode(node, currentNodeId, handler, config);
            ctx.recordResult(currentNodeId, lastResult);

            // ── NODE_EXIT hook ───────────────────────────────────────────────────────────────
            fireHook(HookPhases.NODE_EXIT, node, lastResult, null, null);

            // Push completed compensable nodes onto the saga stack so they can be rolled back
            // if a later node fails. taskGroup children are executed in a separate code path
            // (driveTaskGroup / executeChildSafely) so they never reach this branch.
            if (handler.compensable() && lastResult.status() == NodeStatus.COMPLETED) {
                compensationStack.add(
                        new CompensationRecord(currentNodeId, node, handler, config, lastResult));
            }

            if (lastResult.status() == NodeStatus.FAILED
                    || lastResult.status() == NodeStatus.CANCELLED) {
                // ── NODE_ERROR hook ──────────────────────────────────────────────────────────
                fireHook(HookPhases.NODE_ERROR, node, lastResult, lastResult.message(), null);
                runCompensation();
                this.finished = true;
                WorkflowResult result = buildResult(currentNodeId, lastResult);
                archiveIfNeeded(result);
                // ── WORKFLOW_END hook ────────────────────────────────────────────────────────
                fireHook(HookPhases.WORKFLOW_END, node, lastResult, null, null);
                awaitAsyncHooks();
                return result;
            }

            // COMPLETED — try to route. End-task always terminates the workflow.
            if (NodeTypes.END_TASK.equals(node.type())) {
                this.finished = true;
                WorkflowResult result = buildResult(currentNodeId, lastResult);
                archiveIfNeeded(result);
                // ── WORKFLOW_END hook ────────────────────────────────────────────────────────
                fireHook(HookPhases.WORKFLOW_END, node, lastResult, null, null);
                awaitAsyncHooks();
                return result;
            }
            EdgeDefinition nextEdge =
                    EdgeRouter.pickNextEdge(graph, currentNodeId, lastResult.outcome(), ctx);
            if (nextEdge == null) {
                // No outgoing edge — treat as terminal.
                this.finished = true;
                WorkflowResult result = buildResult(currentNodeId, lastResult);
                archiveIfNeeded(result);
                // ── WORKFLOW_END hook ────────────────────────────────────────────────────────
                fireHook(HookPhases.WORKFLOW_END, node, lastResult, null, null);
                awaitAsyncHooks();
                return result;
            }
            currentNodeId = nextEdge.to();
        }
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
        Map<String, NodeResult> snapshot = ctx == null ? Map.of() : ctx.snapshotResults();
        String current = ctx == null ? null : ctx.currentNodeId();
        return new WorkflowState(finished ? null : current, finished, snapshot, lastEventName);
    }

    // ---------------------------------------------------------------------------------------

    /**
     * Drive a single node through its handler loop. Returns the final non-WAITING result.
     *
     * @param node the node definition.
     * @param qualifier the storage key used by handlers that need to look up this node's result.
     *     For root-level nodes this equals {@code node.id()}; for taskGroup children this is the
     *     composite {@link TaskGroupContract#childKey} qualifier.
     * @param handler pre-resolved handler for this node type.
     * @param config pre-decoded handler config.
     */
    private NodeResult executeNode(
            NodeDefinition node, String qualifier, NodeHandler<Object> handler, Object config) {
        ctx.setCurrentNodeId(qualifier);
        ctx.setCurrentEvent(null);

        NodeResult current = handler.run(ctx, config);
        while (current.status() == NodeStatus.WAITING) {
            current = drive(handler, config, current, node, qualifier);
        }
        return current;
    }

    /**
     * Translate a {@link NodeStatus#WAITING} result into a follow-up event by inspecting the {@link
     * RuntimeIntents} payload keys and performing the requested Temporal operation.
     */
    private NodeResult drive(
            NodeHandler<Object> handler,
            Object config,
            NodeResult waiting,
            NodeDefinition node,
            String qualifier) {
        Map<String, Object> payload = waiting.payload();

        if (payload.containsKey(RuntimeIntents.ACTIVITY)) {
            return driveActivity(handler, config, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.DURATION)) {
            return driveTimer(handler, config, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.CHILDREN)) {
            return driveTaskGroup(handler, config, payload, node, qualifier);
        }
        if (payload.containsKey(RuntimeIntents.SUB_WORKFLOW_ID)) {
            return driveSubProcess(handler, config, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.AWAIT_EVENT)) {
            return driveAwait(handler, config, payload, node);
        }
        return NodeResult.failed("unknown waiting intent: " + payload.keySet());
    }

    private NodeResult driveActivity(
            NodeHandler<Object> handler,
            Object config,
            Map<String, Object> payload,
            NodeDefinition node) {
        String activityName = String.valueOf(payload.get(RuntimeIntents.ACTIVITY));
        @SuppressWarnings("unchecked")
        Map<String, Object> input =
                (Map<String, Object>) payload.get(RuntimeIntents.ACTIVITY_INPUT);
        GenericActivity stub =
                Workflow.newActivityStub(GenericActivity.class, DEFAULT_ACTIVITY_OPTIONS);
        ActivityResult result = stub.execute(new ActivityCall(activityName, input));

        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("status", result.success() ? "success" : "failed");
        if (result.error() != null) {
            eventPayload.put("message", result.error());
        }
        if (result.output() != null) {
            eventPayload.put("output", result.output());
        }
        JsonNode eventJson = mapper.valueToTree(eventPayload);
        WorkflowEvent event =
                new WorkflowEvent(ServiceTaskHandler.ACTIVITY_RESULT_EVENT, eventJson);
        return deliver(handler, config, event, node);
    }

    private NodeResult driveTimer(
            NodeHandler<Object> handler,
            Object config,
            Map<String, Object> payload,
            NodeDefinition node) {
        Duration duration = parseDurationOrThrow(payload.get(RuntimeIntents.DURATION));
        Workflow.sleep(duration);
        WorkflowEvent event = new WorkflowEvent(TimerTaskHandler.FIRED_EVENT, null);
        return deliver(handler, config, event, node);
    }

    private NodeResult driveAwait(
            NodeHandler<Object> handler,
            Object config,
            Map<String, Object> payload,
            NodeDefinition node) {
        Object awaitObj = payload.get(RuntimeIntents.AWAIT_EVENT);
        if (awaitObj == null) return NodeResult.failed("await intent missing event name");

        final String capturedNodeId = ctx.currentNodeId();

        Object timeoutObj = payload.get(RuntimeIntents.TIMEOUT_DURATION);
        Duration timeout = timeoutObj == null ? null : parseDurationOrThrow(timeoutObj);

        WorkflowEvent matched;
        if (timeout == null) {
            Workflow.await(() -> hasMatchingCanAccept(handler, config, capturedNodeId));
            matched = pollMatchingCanAccept(handler, config, capturedNodeId);
        } else {
            boolean got =
                    Workflow.await(
                            timeout, () -> hasMatchingCanAccept(handler, config, capturedNodeId));
            if (!got) {
                String timeoutEventName = resolveTimeoutEventName(handler);
                ctx.setCurrentNodeId(capturedNodeId);
                WorkflowEvent timeoutEvent = new WorkflowEvent(timeoutEventName, null);
                return deliver(handler, config, timeoutEvent, node);
            }
            matched = pollMatchingCanAccept(handler, config, capturedNodeId);
        }
        if (matched == null) return NodeResult.waiting();
        ctx.setCurrentNodeId(capturedNodeId);
        return deliver(handler, config, matched, node);
    }

    private NodeExecutionContext fixedNodeCtx(String nodeId, WorkflowEvent event) {
        return new NodeExecutionContext() {
            @Override
            public String workflowId() {
                return ctx.workflowId();
            }

            @Override
            public String instanceId() {
                return ctx.instanceId();
            }

            @Override
            public String definitionId() {
                return ctx.definitionId();
            }

            @Override
            public String currentNodeId() {
                return nodeId;
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode businessData() {
                return ctx.businessData();
            }

            @Override
            public java.util.Map<String, Object> variables() {
                return ctx.variables();
            }

            @Override
            public org.carl.infrastructure.workflow.definition.NodeResult resultOf(String id) {
                return ctx.resultOf(id);
            }

            @Override
            public org.carl.infrastructure.workflow.spi.WorkflowEvent currentEvent() {
                return event;
            }
        };
    }

    private boolean hasMatchingCanAccept(
            NodeHandler<Object> handler, Object config, String nodeId) {
        for (WorkflowEvent ev : signalQueue) {
            if (handler.canAccept(fixedNodeCtx(nodeId, ev), ev, config)) {
                return true;
            }
        }
        return false;
    }

    private WorkflowEvent pollMatchingCanAccept(
            NodeHandler<Object> handler, Object config, String nodeId) {
        Iterator<WorkflowEvent> it = signalQueue.iterator();
        while (it.hasNext()) {
            WorkflowEvent ev = it.next();
            if (handler.canAccept(fixedNodeCtx(nodeId, ev), ev, config)) {
                it.remove();
                return ev;
            }
        }
        return null;
    }

    private NodeResult driveTaskGroup(
            NodeHandler<Object> handler,
            Object config,
            Map<String, Object> payload,
            NodeDefinition parentNode,
            String parentQualifier) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children =
                (List<Map<String, Object>>) payload.get(RuntimeIntents.CHILDREN);
        if (children == null || children.isEmpty()) {
            WorkflowEvent ev =
                    new WorkflowEvent(
                            org.carl.infrastructure.workflow.handlers.TaskGroupHandler
                                    .CHILD_COMPLETED_EVENT,
                            null);
            return deliver(handler, config, ev, parentNode);
        }

        // Fan-out: each child runs in its own coroutine so they can independently consume
        // signals from signalQueue. Wrapped in a CancellationScope to support short-circuit
        // semantics (ANY: first APPROVED wins; ALL: first REJECTED loses).
        List<Promise<NodeResult>> promises = new ArrayList<>(children.size());
        List<String> childIds = new ArrayList<>(children.size());
        boolean[] consumed = new boolean[children.size()];
        for (Map<String, Object> childSpec : children) {
            String childId = (String) childSpec.get("id");
            childIds.add(childId);
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
                                                () -> executeChildSafely(childNode, qualifier)));
                            }
                        });
        scope.run();

        // Join loop: wait for any pending child to complete, record its result, deliver one
        // child-completed event, then ask the handler whether the group is done. On short-circuit
        // cancel the surrounding scope so any still-pending children stop blocking immediately;
        // their CANCELLED results are drained and recorded for caller inspection.
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
                WorkflowEvent ev =
                        new WorkflowEvent(
                                org.carl.infrastructure.workflow.handlers.TaskGroupHandler
                                        .CHILD_COMPLETED_EVENT,
                                null);
                aggregated = deliver(handler, config, ev, parentNode);
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

    /**
     * Start a child {@link GenericWorkflow} and block (coroutine-suspending) until it completes.
     * The child's final outcome is mapped via {@code SubProcessConfig.outcomeMapping} and delivered
     * to the handler as a {@link SubProcessHandler#COMPLETED_EVENT} event.
     */
    private NodeResult driveSubProcess(
            NodeHandler<Object> handler,
            Object config,
            Map<String, Object> payload,
            NodeDefinition node) {
        String subWorkflowId = String.valueOf(payload.get(RuntimeIntents.SUB_WORKFLOW_ID));
        String childDefJsonString = (String) payload.get(RuntimeIntents.SUB_DEFINITION_JSON);

        if (childDefJsonString == null || childDefJsonString.isBlank()) {
            return NodeResult.failed(
                    "subProcess '" + subWorkflowId + "': definitionJson is required in config");
        }

        // Parse the child workflow definition
        WorkflowDefinition childDefinition;
        try {
            childDefinition = mapper.readValue(childDefJsonString, WorkflowDefinition.class);
        } catch (Exception e) {
            return NodeResult.failed("Failed to parse child definition: " + e.getMessage());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawSubInput =
                (Map<String, Object>) payload.get(RuntimeIntents.SUB_INPUT);

        WorkflowInput childInput = WorkflowInput.from(childDefinition, rawSubInput);

        // Child workflow id: parentId + "/sub/" + currentNodeId + "/" + visitCount.
        String childWorkflowId =
                Workflow.getInfo().getWorkflowId()
                        + "/sub/"
                        + ctx.currentNodeId()
                        + "/"
                        + ctx.nextVisitCount(ctx.currentNodeId());
        GenericWorkflow childStub =
                Workflow.newChildWorkflowStub(
                        GenericWorkflow.class,
                        ChildWorkflowOptions.newBuilder().setWorkflowId(childWorkflowId).build());

        // Synchronous call in Temporal workflow context: suspends this coroutine, starts the
        // child workflow, resumes when the child completes. Deterministic + replay-safe.
        WorkflowResult childResult = childStub.execute(childInput);

        String subOutcome =
                childResult.finalOutcome() != null
                        ? childResult.finalOutcome()
                        : (childResult.finalStatus() != null
                                ? childResult.finalStatus().name()
                                : Outcomes.COMPLETED);
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("subOutcome", subOutcome);
        JsonNode eventJson = mapper.valueToTree(eventPayload);
        WorkflowEvent event = new WorkflowEvent(SubProcessHandler.COMPLETED_EVENT, eventJson);
        return deliver(handler, config, event, node);
    }

    /**
     * Execute a taskGroup child node, translating Temporal cancellation into {@link
     * NodeResult#cancelled()} and any other runtime exception into {@link
     * NodeResult#failed(String)}.
     */
    private NodeResult executeChildSafely(NodeDefinition childNode, String qualifier) {
        try {
            @SuppressWarnings("unchecked")
            NodeHandler<Object> handler = (NodeHandler<Object>) registry.lookup(childNode.type());
            Object config =
                    NodeConfigCodec.decode(mapper, handler, childNode.type(), childNode.config());
            return executeNode(childNode, qualifier, handler, config);
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

    /**
     * Drive saga compensation in reverse completion order. For each compensable node on the stack,
     * calls {@link NodeHandler#compensate} and — when it returns a WAITING+ACTIVITY result —
     * executes the compensating activity. Best-effort: a failed compensation step is logged and
     * skipped so all prior compensable nodes still get a chance to roll back.
     */
    @SuppressWarnings("unchecked")
    private void runCompensation() {
        for (int i = compensationStack.size() - 1; i >= 0; i--) {
            CompensationRecord rec = compensationStack.get(i);
            ctx.setCurrentNodeId(rec.nodeId());
            ctx.setCurrentEvent(null);
            try {
                // ── COMPENSATE hook ──────────────────────────────────────────────────────────
                fireHook(HookPhases.COMPENSATE, rec.node(), rec.completedResult(), null, null);
                NodeResult compResult =
                        rec.handler().compensate(ctx, rec.config(), rec.completedResult());
                if (compResult != null
                        && compResult.status() == NodeStatus.WAITING
                        && compResult.payload() != null
                        && compResult.payload().containsKey(RuntimeIntents.ACTIVITY)) {
                    // Execute the compensating activity (result is intentionally ignored — best
                    // effort; any throw is caught by the outer try/catch).
                    driveActivity(rec.handler(), rec.config(), compResult.payload(), rec.node());
                }
            } catch (RuntimeException e) {
                Workflow.getLogger(GenericWorkflowImpl.class)
                        .warn("compensation for '{}' threw: {}", rec.nodeId(), e.getMessage());
            }
        }
    }

    private NodeResult deliver(
            NodeHandler<Object> handler, Object config, WorkflowEvent event, NodeDefinition node) {
        ctx.setCurrentEvent(event);
        if (event != null) {
            lastEventName = event.name();
        }
        NodeResult next = handler.onEvent(ctx, event, config);
        // ── EVENT hook ───────────────────────────────────────────────────────────────────────
        fireHook(HookPhases.EVENT, node, null, null, event);
        return next == null ? NodeResult.waiting() : next;
    }

    private static String resolveTimeoutEventName(NodeHandler<?> handler) {
        if (handler instanceof ApprovalTaskHandler) {
            return ApprovalTaskHandler.TIMEOUT_EVENT;
        }
        if (handler instanceof UserTaskHandler) {
            return UserTaskHandler.TIMEOUT_EVENT;
        }
        if (handler instanceof EventTaskHandler) {
            return EventTaskHandler.TIMEOUT_EVENT;
        }
        return "_timeout";
    }

    private static Duration parseDurationOrThrow(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("duration intent missing value");
        }
        try {
            return Duration.parse(String.valueOf(raw));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid duration: " + raw, e);
        }
    }


    private WorkflowResult buildResult(String currentNodeId, NodeResult lastResult) {
        return new WorkflowResult(
                currentNodeId,
                lastResult == null ? null : lastResult.outcome(),
                lastResult == null ? NodeStatus.COMPLETED : lastResult.status(),
                ctx.snapshotResults(),
                List.copyOf(ctx.executionRecords()),
                ctx.snapshotVariables());
    }

    // ── Interceptor hook helpers ─────────────────────────────────────────────────────────────────

    /**
     * Fire a lifecycle hook at the given phase.
     *
     * <ol>
     *   <li>Deterministic interceptors are called inline (exceptions are caught + logged).
     *   <li>Async interceptors are dispatched to {@link AsyncInterceptorActivity} via
     *       {@link Async#procedure}; the returned promise is added to {@link #asyncHookPromises}.
     * </ol>
     *
     * <p>When no interceptors are registered this method returns immediately with no overhead (R6).
     */
    private void fireHook(
            String phase,
            NodeDefinition node,
            NodeResult result,
            String errorMessage,
            WorkflowEvent event) {
        WorkflowInterceptorRegistry reg = InterceptorHolder.registry();

        // 1. Inline deterministic interceptors
        if (!reg.deterministic().isEmpty()) {
            InterceptorContext ictx =
                    new SimpleInterceptorContext(
                            ctx.workflowId(),
                            ctx.instanceId(),
                            ctx.definitionId(),
                            ctx.businessData());
            for (DeterministicInterceptor di : reg.deterministic()) {
                try {
                    switch (phase) {
                        case HookPhases.WORKFLOW_START -> di.onWorkflowStart(ictx);
                        case HookPhases.WORKFLOW_END   -> di.onWorkflowEnd(ictx, result);
                        case HookPhases.NODE_ENTER     -> di.onNodeEnter(ictx, node);
                        case HookPhases.NODE_EXIT      -> di.onNodeExit(ictx, node, result);
                        case HookPhases.NODE_ERROR     -> di.onNodeError(ictx, node, errorMessage);
                        case HookPhases.EVENT          -> di.onEvent(ictx, event);
                        case HookPhases.COMPENSATE     -> di.onCompensate(ictx, node, result);
                        default -> {
                            // unknown phase — ignore
                        }
                    }
                } catch (RuntimeException e) {
                    Workflow.getLogger(GenericWorkflowImpl.class)
                            .warn(
                                    "DeterministicInterceptor {} threw on {}: {}",
                                    di.getClass().getSimpleName(),
                                    phase,
                                    e.getMessage());
                }
            }
        }

        // 2. Async interceptors — dispatch to activity (non-blocking)
        if (!reg.async().isEmpty()) {
            AsyncHookInvocation inv =
                    new AsyncHookInvocation(
                            phase,
                            ctx.workflowId(),
                            ctx.instanceId(),
                            ctx.definitionId(),
                            ctx.businessData(),
                            node,
                            result,
                            errorMessage,
                            event);
            AsyncInterceptorActivity stub =
                    Workflow.newActivityStub(
                            AsyncInterceptorActivity.class, INTERCEPTOR_ACTIVITY_OPTIONS);
            asyncHookPromises.add(Async.procedure(stub::invoke, inv));
        }
    }

    /**
     * Wait for all outstanding async hook promises (best-effort). Called before each terminal
     * {@code return} in {@link #execute}. Exceptions from hooks are swallowed so a failing hook
     * never prevents the workflow from completing (R7).
     */
    private void awaitAsyncHooks() {
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

    /** Activity options for archive: short timeout, no retry; best-effort. */
    private static final ActivityOptions ARCHIVE_ACTIVITY_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                    .build();

    /**
     * Fire-and-forget archival on terminal completion.
     *
     * <p>Gated by the per-execution {@link WorkflowInput#archiveEnabled()} flag — no JVM-wide
     * static state. When enabled, schedules the archive activity asynchronously via {@link
     * Async#procedure} and queues the resulting promise into {@link #asyncHookPromises}, which is
     * drained by {@link #awaitAsyncHooks()} with errors swallowed. So archival failures (including
     * "Activity Type Archive not registered" when the worker is misconfigured) never affect the
     * main workflow result.
     *
     * @param result the workflow result to archive; may be null for non-terminal states
     */
    private void archiveIfNeeded(WorkflowResult result) {
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
}
