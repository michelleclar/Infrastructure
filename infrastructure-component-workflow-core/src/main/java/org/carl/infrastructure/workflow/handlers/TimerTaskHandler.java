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

/**
 * Built-in handler for {@code timerTask} nodes.
 *
 * <p>Suspends execution until the runtime fires the timer (via {@value #FIRED_EVENT}) or an
 * explicit cancellation arrives (via {@value #CANCEL_EVENT}). The duration is communicated to the
 * runtime through {@link RuntimeIntents#DURATION} in the {@code run} result payload; the handler
 * itself never starts a timer — that is the runtime's responsibility.
 */
public final class TimerTaskHandler implements NodeHandler<TimerTaskConfig> {

    /** Internal event delivered when the timer fires. */
    public static final String FIRED_EVENT = "_timerFired";

    /** Internal cancel event. */
    public static final String CANCEL_EVENT = "_cancel";

    @Override
    public String type() {
        return NodeTypes.TIMER_TASK;
    }

    @Override
    public Class<TimerTaskConfig> configType() {
        return TimerTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.TRIGGERED, Outcomes.CANCELLED);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, TimerTaskConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (config != null && config.duration() != null) {
            payload.put(RuntimeIntents.DURATION, config.duration());
        }
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent ev, TimerTaskConfig cfg) {
        if (ev == null) {
            return false;
        }
        return FIRED_EVENT.equals(ev.name()) || CANCEL_EVENT.equals(ev.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent ev, TimerTaskConfig cfg) {
        if (ev == null) {
            return NodeResult.waiting();
        }
        if (FIRED_EVENT.equals(ev.name())) {
            return NodeResult.completed(Outcomes.TRIGGERED);
        }
        if (CANCEL_EVENT.equals(ev.name())) {
            return NodeResult.completed(Outcomes.CANCELLED);
        }
        return NodeResult.waiting();
    }
}
