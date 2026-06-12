package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;

import java.util.Set;

/**
 * Built-in handler for {@code endTask} nodes.
 *
 * <p>Always completes immediately with {@link Outcomes#COMPLETED}; the runtime is expected to
 * terminate the workflow instance when an end task completes.
 */
public final class EndTaskHandler implements NodeHandler<EndTaskConfig, Object, Object> {

    @Override
    public String type() {
        return NodeTypes.END_TASK;
    }

    @Override
    public Class<EndTaskConfig> configType() {
        return EndTaskConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.COMPLETED);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, EndTaskConfig config) {
        return NodeResult.completed(Outcomes.COMPLETED);
    }
}
