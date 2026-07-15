package org.carl.infra.workflow.handlers;

import org.carl.infra.workflow.definition.NodeResult;
import org.carl.infra.workflow.spi.NodeExecutionContext;
import org.carl.infra.workflow.spi.NodeHandler;
import org.carl.infra.workflow.spi.NodeTypes;

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
