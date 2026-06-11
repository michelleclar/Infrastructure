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
 * Built-in handler for {@code serviceTask} nodes.
 *
 * <p>The handler never invokes an activity directly. {@link #run(NodeExecutionContext,
 * ServiceTaskConfig)} returns {@link NodeResult#waiting()} with the activity name and input
 * communicated via {@link RuntimeIntents}. The runtime is expected to invoke the activity and
 * deliver the result back via an internal {@code "_activityResult"} event whose payload contains
 * {@code "status"} ({@code "success"}/{@code "failed"}) and an optional {@code "message"}.
 */
public final class ServiceTaskHandler implements NodeHandler<ServiceTaskConfig> {

    /** Internal event name used by the runtime to deliver the activity result. */
    public static final String ACTIVITY_RESULT_EVENT = "_activityResult";

    @Override
    public String type() {
        return NodeTypes.SERVICE_TASK;
    }

    @Override
    public Class<ServiceTaskConfig> configType() {
        return ServiceTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.SUCCESS, Outcomes.FAILED);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, ServiceTaskConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (config != null) {
            payload.put(RuntimeIntents.ACTIVITY, config.activity());
            if (config.activityInput() != null) {
                payload.put(RuntimeIntents.ACTIVITY_INPUT, config.activityInput());
            }
        }
        return new NodeResult(
                org.carl.infrastructure.workflow.definition.NodeStatus.WAITING,
                null,
                payload,
                null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent event, ServiceTaskConfig cfg) {
        return event != null && ACTIVITY_RESULT_EVENT.equals(event.name());
    }

    @Override
    public NodeResult onEvent(
            NodeExecutionContext ctx, WorkflowEvent event, ServiceTaskConfig config) {
        if (event == null || !ACTIVITY_RESULT_EVENT.equals(event.name())) {
            return NodeResult.waiting();
        }
        JsonNode payload = event.payload();
        if (payload == null) {
            // No payload means the runtime delivered the result event without status detail;
            // equivalent to a missing status field, which is treated as failure below.
            return NodeResult.failed("activity failed");
        }
        String status = payload.path("status").asText(null);
        if ("success".equalsIgnoreCase(status)) {
            JsonNode outputNode = payload.get("output");
            // Propagate the activity's output into the NodeResult payload so downstream nodes
            // can read it via ${results['nodeId'].output} in condition expressions.
            if (outputNode != null && !outputNode.isNull()) {
                return NodeResult.completed(Outcomes.SUCCESS, Map.of("output", outputNode));
            }
            return NodeResult.completed(Outcomes.SUCCESS);
        }
        // Any non-success status (or missing status field) is treated as failure. The runtime
        // message takes priority over the generic fallback to preserve the root-cause detail.
        String message = payload.path("message").asText("activity failed");
        if (message.isEmpty()) {
            message = "activity failed";
        }
        return NodeResult.failed(message);
    }

    @Override
    public boolean compensable() {
        return true;
    }

    /**
     * If {@link ServiceTaskConfig#compensateActivity()} is configured, returns a WAITING result
     * with an {@link RuntimeIntents#ACTIVITY} intent so the runtime invokes the rollback activity.
     * Otherwise returns a no-op COMPLETED result.
     */
    @Override
    public NodeResult compensate(
            NodeExecutionContext ctx, ServiceTaskConfig config, NodeResult completedResult) {
        if (config == null
                || config.compensateActivity() == null
                || config.compensateActivity().isBlank()) {
            return NodeResult.completed(Outcomes.SUCCESS);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(RuntimeIntents.ACTIVITY, config.compensateActivity());
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }
}
