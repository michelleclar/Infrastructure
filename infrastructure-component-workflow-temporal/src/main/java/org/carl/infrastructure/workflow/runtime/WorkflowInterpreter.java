package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.engine.EdgeRouter;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.handlers.ApprovalTaskHandler;
import org.carl.infrastructure.workflow.handlers.EventTaskHandler;
import org.carl.infrastructure.workflow.handlers.RuntimeIntents;
import org.carl.infrastructure.workflow.handlers.ServiceTaskHandler;
import org.carl.infrastructure.workflow.handlers.SubProcessHandler;
import org.carl.infrastructure.workflow.handlers.TaskGroupContract;
import org.carl.infrastructure.workflow.handlers.TaskGroupHandler;
import org.carl.infrastructure.workflow.handlers.TimerTaskHandler;
import org.carl.infrastructure.workflow.handlers.UserTaskHandler;
import org.carl.infrastructure.workflow.interceptor.DeterministicInterceptor;
import org.carl.infrastructure.workflow.interceptor.InterceptorContext;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pure orchestration engine: interprets a {@link WorkflowDefinition} with the registered {@link
 * NodeHandler} set, driving the node loop, WAITING-intent dispatch, outcome routing, saga
 * compensation and deterministic interceptor hooks.
 *
 * <p><strong>Temporal-free by construction.</strong> Every side effect (activity invocation, timer,
 * signal await, taskGroup fan-out, child workflow, async-hook dispatch, archival) is delegated to a
 * {@link RuntimeOps} port. The Temporal adapter ({@link GenericWorkflowImpl}) supplies a Temporal
 * implementation; tests supply an in-memory fake. The interpreter therefore unit-tests on a plain
 * JVM with no Temporal runtime.
 *
 * <p>The {@link ExecutionContext} is shared with the adapter (same instance) so the adapter's
 * {@code runTaskGroup} implementation can record child results against the same state.
 */
final class WorkflowInterpreter {

    private final WorkflowDefinition definition;
    private final WorkflowGraph graph;
    private final NodeHandlerRegistry registry;
    private final ObjectMapper mapper;
    private final ExecutionContext ctx;
    private final RuntimeOps ops;
    private final WorkflowInterceptorRegistry interceptors;

    private final List<CompensationRecord> compensationStack = new ArrayList<>();
    private boolean finished;
    private String lastEventName;

    WorkflowInterpreter(
            WorkflowDefinition definition,
            WorkflowGraph graph,
            NodeHandlerRegistry registry,
            ObjectMapper mapper,
            ExecutionContext ctx,
            RuntimeOps ops,
            WorkflowInterceptorRegistry interceptors) {
        this.definition = definition;
        this.graph = graph;
        this.registry = registry;
        this.mapper = mapper;
        this.ctx = ctx;
        this.ops = ops;
        this.interceptors = interceptors;
    }

    /** Tracks a successfully-completed compensable node for potential saga rollback. */
    private record CompensationRecord(
            String nodeId,
            NodeDefinition node,
            @SuppressWarnings("rawtypes") NodeHandler handler,
            Object config,
            Object state,
            NodeResult completedResult) {}

    // ── Main loop ────────────────────────────────────────────────────────────────────────────

    WorkflowResult run(String requestedStartNode) {
        fireHook(HookPhases.WORKFLOW_START, null, null, null, null);

        String currentNodeId = EdgeRouter.resolveStartNode(requestedStartNode, definition, graph);

        while (true) {
            NodeDefinition node = graph.node(currentNodeId);
            @SuppressWarnings("unchecked")
            NodeHandler<Object, Object, Object> handler =
                    (NodeHandler<Object, Object, Object>) registry.lookup(node.type());
            Object config = NodeConfigCodec.decode(mapper, handler, node.config());
            Object state = NodeConfigCodec.decodeState(mapper, handler, ctx.businessData());

            fireHook(HookPhases.NODE_ENTER, node, null, null, null);

            NodeResult lastResult = executeNode(node, currentNodeId, handler, config, state);
            ctx.recordResult(currentNodeId, lastResult);

            fireHook(HookPhases.NODE_EXIT, node, lastResult, null, null);

            if (handler.compensable() && lastResult.status() == NodeStatus.COMPLETED) {
                compensationStack.add(
                        new CompensationRecord(
                                currentNodeId, node, handler, config, state, lastResult));
            }

            if (lastResult.status() == NodeStatus.FAILED
                    || lastResult.status() == NodeStatus.CANCELLED) {
                fireHook(HookPhases.NODE_ERROR, node, lastResult, lastResult.message(), null);
                runCompensation();
                return terminate(currentNodeId, lastResult, node);
            }

            if (NodeTypes.END_TASK.equals(node.type())) {
                return terminate(currentNodeId, lastResult, node);
            }

            EdgeDefinition nextEdge =
                    EdgeRouter.pickNextEdge(graph, currentNodeId, lastResult.outcome(), ctx);
            if (nextEdge == null) {
                return terminate(currentNodeId, lastResult, node);
            }
            currentNodeId = nextEdge.to();
        }
    }

    private WorkflowResult terminate(
            String currentNodeId, NodeResult lastResult, NodeDefinition node) {
        this.finished = true;
        WorkflowResult result = buildResult(currentNodeId, lastResult);
        ops.archive(result);
        fireHook(HookPhases.WORKFLOW_END, node, lastResult, null, null);
        ops.awaitAsyncHooks();
        return result;
    }

    /** Snapshot for the adapter's {@code @QueryMethod}. */
    WorkflowState snapshotState() {
        return new WorkflowState(
                finished ? null : ctx.currentNodeId(),
                finished,
                ctx.snapshotResults(),
                lastEventName);
    }

    // ── Node execution ───────────────────────────────────────────────────────────────────────

    /** Drive a single node through its handler loop until a non-WAITING result. */
    private NodeResult executeNode(
            NodeDefinition node,
            String qualifier,
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state) {
        ctx.setCurrentNodeId(qualifier);
        ctx.setCurrentEvent(null);

        NodeResult current = handler.run(ctx, config, state);
        while (current.status() == NodeStatus.WAITING) {
            current = drive(handler, config, state, current, node, qualifier);
        }
        return current;
    }

    /** Lookup + decode + execute a taskGroup child (invoked by the adapter via a callback). */
    private NodeResult executeChild(NodeDefinition childNode, String qualifier) {
        @SuppressWarnings("unchecked")
        NodeHandler<Object, Object, Object> handler =
                (NodeHandler<Object, Object, Object>) registry.lookup(childNode.type());
        Object config = NodeConfigCodec.decode(mapper, handler, childNode.config());
        Object state = NodeConfigCodec.decodeState(mapper, handler, ctx.businessData());
        return executeNode(childNode, qualifier, handler, config, state);
    }

    /** Translate a WAITING result into the matching side effect via {@link RuntimeOps}. */
    private NodeResult drive(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            NodeResult waiting,
            NodeDefinition node,
            String qualifier) {
        Map<String, Object> payload = waiting.payload();

        if (payload.containsKey(RuntimeIntents.ACTIVITY)) {
            return driveActivity(handler, config, state, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.DURATION)) {
            return driveTimer(handler, config, state, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.CHILDREN)) {
            return driveTaskGroup(handler, config, state, payload, node, qualifier);
        }
        if (payload.containsKey(RuntimeIntents.SUB_WORKFLOW_ID)) {
            return driveSubProcess(handler, config, state, payload, node);
        }
        if (payload.containsKey(RuntimeIntents.AWAIT_EVENT)) {
            return driveAwait(handler, config, state, payload, node);
        }
        return NodeResult.failed("unknown waiting intent: " + payload.keySet());
    }

    private NodeResult driveActivity(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            Map<String, Object> payload,
            NodeDefinition node) {
        String activityName = String.valueOf(payload.get(RuntimeIntents.ACTIVITY));
        @SuppressWarnings("unchecked")
        Map<String, Object> input =
                (Map<String, Object>) payload.get(RuntimeIntents.ACTIVITY_INPUT);

        ActivityResult result = ops.runActivity(activityName, input);

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
        return deliver(handler, config, state, event, node);
    }

    private NodeResult driveTimer(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            Map<String, Object> payload,
            NodeDefinition node) {
        Duration duration = parseDurationOrThrow(payload.get(RuntimeIntents.DURATION));
        ops.sleep(duration);
        return deliver(
                handler, config, state, new WorkflowEvent(TimerTaskHandler.FIRED_EVENT, null), node);
    }

    private NodeResult driveAwait(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            Map<String, Object> payload,
            NodeDefinition node) {
        Object awaitObj = payload.get(RuntimeIntents.AWAIT_EVENT);
        if (awaitObj == null) {
            return NodeResult.failed("await intent missing event name");
        }
        final String capturedNodeId = ctx.currentNodeId();

        Object timeoutObj = payload.get(RuntimeIntents.TIMEOUT_DURATION);
        Duration timeout = timeoutObj == null ? null : parseDurationOrThrow(timeoutObj);

        RuntimeOps.EventMatcher matcher =
                ev -> {
                    NodeExecutionContext eventCtx = fixedNodeCtx(capturedNodeId, ev);
                    if (!handler.canAccept(eventCtx, ev, config)) {
                        return false;
                    }
                    Object eventPayload =
                            ev == null
                                    ? null
                                    : NodeConfigCodec.decodeEventPayload(
                                            mapper, handler, ev.payload());
                    return handler.canAccept(eventCtx, ev, config, eventPayload);
                };
        RuntimeOps.AwaitOutcome outcome = ops.awaitEvent(matcher, timeout);

        if (outcome.matched() != null) {
            ctx.setCurrentNodeId(capturedNodeId);
            return deliver(handler, config, state, outcome.matched(), node);
        }
        if (outcome.timedOut()) {
            ctx.setCurrentNodeId(capturedNodeId);
            return deliver(
                    handler,
                    config,
                    state,
                    new WorkflowEvent(resolveTimeoutEventName(handler), null),
                    node);
        }
        return NodeResult.waiting();
    }

    private NodeResult driveSubProcess(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            Map<String, Object> payload,
            NodeDefinition node) {
        String subWorkflowId = String.valueOf(payload.get(RuntimeIntents.SUB_WORKFLOW_ID));
        String childDefJsonString = (String) payload.get(RuntimeIntents.SUB_DEFINITION_JSON);
        if (childDefJsonString == null || childDefJsonString.isBlank()) {
            return NodeResult.failed(
                    "subProcess '" + subWorkflowId + "': definitionJson is required in config");
        }
        WorkflowDefinition childDefinition;
        try {
            childDefinition = mapper.readValue(childDefJsonString, WorkflowDefinition.class);
        } catch (Exception e) {
            return NodeResult.failed("Failed to parse child definition: " + e.getMessage());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawSubInput =
                (Map<String, Object>) payload.get(RuntimeIntents.SUB_INPUT);

        String idHint = ctx.currentNodeId() + "/" + ctx.nextVisitCount(ctx.currentNodeId());
        WorkflowResult childResult = ops.runSubProcess(childDefinition, rawSubInput, idHint);

        String subOutcome =
                childResult.finalOutcome() != null
                        ? childResult.finalOutcome()
                        : (childResult.finalStatus() != null
                                ? childResult.finalStatus().name()
                                : "COMPLETED");
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("subOutcome", subOutcome);
        JsonNode eventJson = mapper.valueToTree(eventPayload);
        WorkflowEvent event = new WorkflowEvent(SubProcessHandler.COMPLETED_EVENT, eventJson);
        return deliver(handler, config, state, event, node);
    }

    /**
     * Drive a taskGroup. The interpreter owns the join policy: it parses children, fans them out
     * via {@link RuntimeOps#fanOut} (the adapter's concurrency primitive), then loops — recording
     * each completed child and asking the taskGroup handler ({@code onEvent} via {@link #deliver})
     * whether the group is done. A non-WAITING aggregate short-circuits: the still-pending children
     * are cancelled and their (CANCELLED) results drained for caller inspection.
     */
    private NodeResult driveTaskGroup(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            Map<String, Object> payload,
            NodeDefinition node,
            String parentQualifier) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children =
                (List<Map<String, Object>>) payload.get(RuntimeIntents.CHILDREN);
        if (children == null || children.isEmpty()) {
            return deliver(
                    handler,
                    config,
                    state,
                    new WorkflowEvent(TaskGroupHandler.CHILD_COMPLETED_EVENT, null),
                    node);
        }

        int n = children.size();
        List<String> childQualifiers = new ArrayList<>(n);
        List<Supplier<NodeResult>> tasks = new ArrayList<>(n);
        for (Map<String, Object> childSpec : children) {
            String childId = (String) childSpec.get("id");
            String childType = (String) childSpec.get("type");
            String childLabel = (String) childSpec.get("label");
            Object rawConfig = childSpec.get("config");
            JsonNode childConfig = rawConfig == null ? null : mapper.valueToTree(rawConfig);
            NodeDefinition childNode =
                    new NodeDefinition(childId, childLabel, childType, null, childConfig);
            String childQualifier = TaskGroupContract.childKey(parentQualifier, childId);
            childQualifiers.add(childQualifier);
            tasks.add(() -> executeChild(childNode, childQualifier));
        }

        RuntimeOps.TaskGroupScope scope = ops.fanOut(tasks);
        boolean[] consumed = new boolean[n];
        NodeResult aggregated = NodeResult.waiting();
        int remaining = n;
        while (remaining > 0) {
            boolean anyPending = false;
            for (int i = 0; i < n; i++) {
                if (!consumed[i]) {
                    anyPending = true;
                    break;
                }
            }
            if (!anyPending) {
                break;
            }

            scope.awaitAny();

            for (int i = 0; i < n; i++) {
                if (consumed[i] || !scope.isChildCompleted(i)) {
                    continue;
                }
                ctx.recordResult(childQualifiers.get(i), scope.result(i));
                consumed[i] = true;
                remaining--;

                ctx.setCurrentNodeId(parentQualifier);
                aggregated =
                        deliver(
                                handler,
                                config,
                                state,
                                new WorkflowEvent(TaskGroupHandler.CHILD_COMPLETED_EVENT, null),
                                node);
                if (aggregated.status() != NodeStatus.WAITING) {
                    scope.cancelAll(
                            "taskGroup '"
                                    + parentQualifier
                                    + "' short-circuited with outcome "
                                    + aggregated.outcome());
                    for (int j = 0; j < n; j++) {
                        if (consumed[j]) {
                            continue;
                        }
                        ctx.recordResult(childQualifiers.get(j), scope.result(j));
                        consumed[j] = true;
                    }
                    return aggregated;
                }
            }
        }
        return aggregated;
    }

    // ── Event delivery ───────────────────────────────────────────────────────────────────────

    private NodeResult deliver(
            NodeHandler<Object, Object, Object> handler,
            Object config,
            Object state,
            WorkflowEvent event,
            NodeDefinition node) {
        ctx.setCurrentEvent(event);
        if (event != null) {
            lastEventName = event.name();
        }
        Object eventPayload =
                event == null
                        ? null
                        : NodeConfigCodec.decodeEventPayload(mapper, handler, event.payload());
        NodeResult next = handler.onEvent(ctx, event, config, eventPayload);
        fireHook(HookPhases.EVENT, node, null, null, event);
        return next == null ? NodeResult.waiting() : next;
    }

    // ── Saga compensation ────────────────────────────────────────────────────────────────────

    private void runCompensation() {
        for (int i = compensationStack.size() - 1; i >= 0; i--) {
            CompensationRecord rec = compensationStack.get(i);
            ctx.setCurrentNodeId(rec.nodeId());
            ctx.setCurrentEvent(null);
            try {
                fireHook(HookPhases.COMPENSATE, rec.node(), rec.completedResult(), null, null);
                @SuppressWarnings("unchecked")
                NodeResult compResult =
                        rec.handler()
                                .compensate(
                                        ctx, rec.config(), rec.completedResult(), rec.state());
                if (compResult != null
                        && compResult.status() == NodeStatus.WAITING
                        && compResult.payload() != null
                        && compResult.payload().containsKey(RuntimeIntents.ACTIVITY)) {
                    // Best-effort compensating activity; result intentionally ignored.
                    Map<String, Object> p = compResult.payload();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> input =
                            (Map<String, Object>) p.get(RuntimeIntents.ACTIVITY_INPUT);
                    ops.runActivity(String.valueOf(p.get(RuntimeIntents.ACTIVITY)), input);
                }
            } catch (RuntimeException e) {
                ops.warn("compensation for '" + rec.nodeId() + "' threw: " + e.getMessage());
            }
        }
    }

    // ── Interceptor hooks ────────────────────────────────────────────────────────────────────

    private void fireHook(
            String phase,
            NodeDefinition node,
            NodeResult result,
            String errorMessage,
            WorkflowEvent event) {
        // 1. Inline deterministic interceptors (pure).
        if (!interceptors.deterministic().isEmpty()) {
            InterceptorContext ictx =
                    new SimpleInterceptorContext(
                            ctx.workflowId(),
                            ctx.instanceId(),
                            ctx.definitionId(),
                            ctx.businessData());
            for (DeterministicInterceptor di : interceptors.deterministic()) {
                try {
                    switch (phase) {
                        case HookPhases.WORKFLOW_START -> di.onWorkflowStart(ictx);
                        case HookPhases.WORKFLOW_END -> di.onWorkflowEnd(ictx, result);
                        case HookPhases.NODE_ENTER -> di.onNodeEnter(ictx, node);
                        case HookPhases.NODE_EXIT -> di.onNodeExit(ictx, node, result);
                        case HookPhases.NODE_ERROR -> di.onNodeError(ictx, node, errorMessage);
                        case HookPhases.EVENT -> di.onEvent(ictx, event);
                        case HookPhases.COMPENSATE -> di.onCompensate(ictx, node, result);
                        default -> {
                            /* unknown phase — ignore */
                        }
                    }
                } catch (RuntimeException e) {
                    ops.warn(
                            "DeterministicInterceptor "
                                    + di.getClass().getSimpleName()
                                    + " threw on "
                                    + phase
                                    + ": "
                                    + e.getMessage());
                }
            }
        }

        // 2. Async interceptors — dispatch via the port (non-blocking).
        if (!interceptors.async().isEmpty()) {
            ops.emitAsyncHook(
                    new AsyncHookInvocation(
                            phase,
                            ctx.workflowId(),
                            ctx.instanceId(),
                            ctx.definitionId(),
                            ctx.businessData(),
                            node,
                            result,
                            errorMessage,
                            event));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    private WorkflowResult buildResult(String currentNodeId, NodeResult lastResult) {
        return new WorkflowResult(
                currentNodeId,
                lastResult == null ? null : lastResult.outcome(),
                lastResult == null ? NodeStatus.COMPLETED : lastResult.status(),
                ctx.snapshotResults(),
                List.copyOf(ctx.executionRecords()),
                ctx.snapshotVariables());
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
            public JsonNode businessData() {
                return ctx.businessData();
            }

            @Override
            public Map<String, Object> variables() {
                return ctx.variables();
            }

            @Override
            public NodeResult resultOf(String id) {
                return ctx.resultOf(id);
            }

            @Override
            public WorkflowEvent currentEvent() {
                return event;
            }
        };
    }

    private static String resolveTimeoutEventName(NodeHandler<?, ?, ?> handler) {
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
}
