package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;

/**
 * Built-in handler for {@code endTask} nodes.
 *
 * <p>Always completes immediately with {@code "COMPLETED"}; the runtime is expected to
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
    public NodeResult run(NodeExecutionContext ctx, EndTaskConfig config) {
        return NodeResult.completed("COMPLETED");
    }
}
