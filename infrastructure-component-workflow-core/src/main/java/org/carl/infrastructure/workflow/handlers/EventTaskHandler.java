package org.carl.infrastructure.workflow.handlers;

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

/** Built-in handler for {@code eventTask} nodes. */
public final class EventTaskHandler implements NodeHandler<EventTaskConfig> {

    /** Internal timeout event delivered by the runtime. */
    public static final String TIMEOUT_EVENT = "_timeout";

    @Override
    public String type() {
        return NodeTypes.EVENT_TASK;
    }

    @Override
    public Class<EventTaskConfig> configType() {
        return EventTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.RECEIVED, Outcomes.TIMEOUT);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, EventTaskConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (config != null) {
            if (config.awaitEvent() != null) {
                payload.put(RuntimeIntents.AWAIT_EVENT, config.awaitEvent());
            }
            if (config.timeoutDuration() != null) {
                payload.put(RuntimeIntents.TIMEOUT_DURATION, config.timeoutDuration());
            }
        }
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }

    @Override
    public boolean canAccept(
            NodeExecutionContext ctx, WorkflowEvent event, EventTaskConfig config) {
        if (event == null) {
            return false;
        }
        if (TIMEOUT_EVENT.equals(event.name())) {
            return true;
        }
        return config != null
                && config.awaitEvent() != null
                && config.awaitEvent().equals(event.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent event, EventTaskConfig cfg) {
        if (event == null) {
            return NodeResult.waiting();
        }
        if (TIMEOUT_EVENT.equals(event.name())) {
            return NodeResult.completed(Outcomes.TIMEOUT);
        }
        if (cfg != null && cfg.awaitEvent() != null && cfg.awaitEvent().equals(event.name())) {
            return NodeResult.completed(Outcomes.RECEIVED);
        }
        return NodeResult.waiting();
    }
}
