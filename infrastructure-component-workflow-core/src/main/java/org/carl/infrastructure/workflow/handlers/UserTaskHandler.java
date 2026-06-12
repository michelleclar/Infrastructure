package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Built-in handler for generic {@code userTask} nodes.
 *
 * <p>Mirrors the {@link ApprovalTaskHandler} shape but emits the generic {@link
 * Outcomes#COMPLETED}/{@link Outcomes#CANCELLED}/{@link Outcomes#TIMEOUT} outcome set. The {@code
 * decision} field on the await event maps to {@code completed}, {@code cancelled} (case
 * insensitive).
 *
 * <p>Like {@link ApprovalTaskHandler}, supports an optional {@value #PAYLOAD_TASK_ID} field in the
 * event payload to disambiguate sibling user tasks that share an {@code awaitEvent} name. See
 * {@link ApprovalTaskHandler} for the detailed semantics.
 */
public final class UserTaskHandler implements NodeHandler<UserTaskConfig, Object, Object> {

    /** Default event name when {@link UserTaskConfig#awaitEvent()} is null/blank. */
    public static final String DEFAULT_AWAIT_EVENT = "userTask";

    /** Internal timeout event delivered by the runtime. */
    public static final String TIMEOUT_EVENT = "_timeout";

    /**
     * Optional payload field used to address a specific task instance when multiple sibling user
     * tasks share the same {@code awaitEvent} name. Value may be the qualified id or the short
     * child id.
     */
    public static final String PAYLOAD_TASK_ID = "taskId";

    @Override
    public String type() {
        return NodeTypes.USER_TASK;
    }

    @Override
    public Class<UserTaskConfig> configType() {
        return UserTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.COMPLETED, Outcomes.CANCELLED, Outcomes.TIMEOUT);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, UserTaskConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (config != null) {
            if (config.assignee() != null) {
                payload.put(RuntimeIntents.ASSIGNEE, config.assignee());
            }
            payload.put(RuntimeIntents.AWAIT_EVENT, awaitEventName(config));
            if (config.timeoutDuration() != null) {
                payload.put(RuntimeIntents.TIMEOUT_DURATION, config.timeoutDuration());
            }
        } else {
            payload.put(RuntimeIntents.AWAIT_EVENT, DEFAULT_AWAIT_EVENT);
        }
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, UserTaskConfig config) {
        if (event == null) {
            return false;
        }
        // Timeout is accepted before the awaitEvent check — mirrors ApprovalTaskHandler to ensure
        // _timeout is never silently dropped even if the awaitEvent name changed.
        if (TIMEOUT_EVENT.equals(event.name())) {
            return true;
        }
        if (!awaitEventName(config).equals(event.name())) {
            return false;
        }
        return matchesTaskId(ctx, event);
    }

    /**
     * Mirrors {@link ApprovalTaskHandler}: if the event payload carries a {@value #PAYLOAD_TASK_ID}
     * field, require it to address this node. Absent payload preserves the legacy single-task
     * contract.
     */
    private static boolean matchesTaskId(NodeExecutionContext ctx, WorkflowEvent event) {
        JsonNode payload = event.payload();
        if (payload == null || !payload.has(PAYLOAD_TASK_ID)) {
            return true;
        }
        String evTaskId = payload.path(PAYLOAD_TASK_ID).asText(null);
        if (evTaskId == null || evTaskId.isBlank()) {
            return true;
        }
        String currentNodeId = ctx == null ? null : ctx.currentNodeId();
        if (currentNodeId == null || currentNodeId.isBlank()) {
            return true;
        }
        if (evTaskId.equals(currentNodeId)) {
            return true;
        }
        return currentNodeId.endsWith("/" + evTaskId);
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, UserTaskConfig cfg) {
        if (event == null) {
            return NodeResult.waiting();
        }
        if (TIMEOUT_EVENT.equals(event.name())) {
            return NodeResult.completed(Outcomes.TIMEOUT);
        }
        if (!awaitEventName(cfg).equals(event.name())) {
            return NodeResult.waiting();
        }
        JsonNode payload = event.payload();
        String decision = payload == null ? null : payload.path("decision").asText(null);
        if (decision == null) {
            return NodeResult.failed("user task event missing 'decision' field");
        }
        // Normalise to lower-case so clients can send "Completed", "CANCELLED", etc.
        switch (decision.trim().toLowerCase()) {
            case "completed":
                return NodeResult.completed(Outcomes.COMPLETED);
            case "cancelled":
                return NodeResult.completed(Outcomes.CANCELLED);
            default:
                return NodeResult.failed("unknown user task decision: " + decision);
        }
    }

    private static String awaitEventName(UserTaskConfig config) {
        if (config == null || config.awaitEvent() == null || config.awaitEvent().isBlank()) {
            return DEFAULT_AWAIT_EVENT;
        }
        return config.awaitEvent();
    }
}
