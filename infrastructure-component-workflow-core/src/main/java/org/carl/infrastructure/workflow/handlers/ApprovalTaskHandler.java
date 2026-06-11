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
 * Built-in handler for {@code approvalTask} nodes.
 *
 * <p>Defers task creation to the runtime via {@link RuntimeIntents}. The runtime is expected to
 * signal completion with an event whose name is {@link ApprovalTaskConfig#awaitEvent()} (default
 * {@value #DEFAULT_AWAIT_EVENT}) and whose payload carries a {@code "decision"} field ({@code
 * "approved"}/{@code "rejected"}/{@code "sendback"}). A {@value #TIMEOUT_EVENT} event yields {@link
 * Outcomes#TIMEOUT}.
 *
 * <h2>Multi-approver disambiguation (task-group children)</h2>
 *
 * When the same {@code awaitEvent} name is used by multiple sibling approval tasks (e.g. an
 * "approval" event consumed by both an {@code hrApproval} and a {@code managerApproval} child of
 * the same {@code taskGroup}), the event payload may carry an optional {@value #PAYLOAD_TASK_ID}
 * field. If present, {@link #canAccept} requires it to identify the current node (either the
 * qualified id from {@link NodeExecutionContext#currentNodeId()} or the short child id, e.g. {@code
 * "approvals/hrApproval"} or just {@code "hrApproval"}). If the payload is absent or has no {@value
 * #PAYLOAD_TASK_ID} field, the legacy name-only match applies for backward compatibility.
 */
public final class ApprovalTaskHandler implements NodeHandler<ApprovalTaskConfig> {

    /** Default event name when {@link ApprovalTaskConfig#awaitEvent()} is null/blank. */
    public static final String DEFAULT_AWAIT_EVENT = "approval";

    /** Internal timeout event delivered by the runtime. */
    public static final String TIMEOUT_EVENT = "_timeout";

    /**
     * Optional payload field used to address a specific task instance when several sibling tasks
     * share the same {@code awaitEvent} name. Value may be the qualified id (e.g. {@code
     * "approvals/hrApproval"}) or just the child short id (e.g. {@code "hrApproval"}).
     */
    public static final String PAYLOAD_TASK_ID = "taskId";

    @Override
    public String type() {
        return NodeTypes.APPROVAL_TASK;
    }

    @Override
    public Class<ApprovalTaskConfig> configType() {
        return ApprovalTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.APPROVED, Outcomes.REJECTED, Outcomes.SENDBACK, Outcomes.TIMEOUT);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, ApprovalTaskConfig config) {
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
    public boolean canAccept(
            NodeExecutionContext ctx, WorkflowEvent event, ApprovalTaskConfig config) {
        if (event == null) {
            return false;
        }
        // Timeout is accepted before the awaitEvent check: the runtime may fire _timeout even
        // when the awaitEvent name differs (e.g. after config hot-swap), and we must not miss it.
        if (TIMEOUT_EVENT.equals(event.name())) {
            return true;
        }
        if (!awaitEventName(config).equals(event.name())) {
            return false;
        }
        return matchesTaskId(ctx, event);
    }

    /**
     * If the event payload carries a {@value #PAYLOAD_TASK_ID} field, require it to address this
     * node; otherwise accept (legacy single-task contract). When {@code ctx} is null or has no
     * current node, name-only matching is preserved (handler unit tests rely on this).
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
        // Tolerate short id form (payload taskId="hrApproval" while ctx is "approvals/hrApproval").
        return currentNodeId.endsWith("/" + evTaskId);
    }

    @Override
    public NodeResult onEvent(
            NodeExecutionContext ctx, WorkflowEvent event, ApprovalTaskConfig config) {
        if (event == null) {
            return NodeResult.waiting();
        }
        if (TIMEOUT_EVENT.equals(event.name())) {
            return NodeResult.completed(Outcomes.TIMEOUT);
        }
        if (!awaitEventName(config).equals(event.name())) {
            return NodeResult.waiting();
        }
        JsonNode payload = event.payload();
        String decision = payload == null ? null : payload.path("decision").asText(null);
        if (decision == null) {
            return NodeResult.failed("approval event missing 'decision' field");
        }
        // Normalise to lower-case so clients can send "Approved", "APPROVED", etc.
        switch (decision.trim().toLowerCase()) {
            case "approved":
                return NodeResult.completed(Outcomes.APPROVED);
            case "rejected":
                return NodeResult.completed(Outcomes.REJECTED);
            case "sendback":
                return NodeResult.completed(Outcomes.SENDBACK);
            default:
                return NodeResult.failed("unknown approval decision: " + decision);
        }
    }

    private static String awaitEventName(ApprovalTaskConfig config) {
        if (config == null || config.awaitEvent() == null || config.awaitEvent().isBlank()) {
            return DEFAULT_AWAIT_EVENT;
        }
        return config.awaitEvent();
    }
}
