package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.ConditionEvaluator;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;

/**
 * Built-in handler for {@code gateway} nodes.
 *
 * <p>Synchronous: evaluates branches in order using {@link ConditionEvaluator}; the first branch
 * whose condition is {@code true} wins. Falls back to {@link GatewayConfig#defaultOutcome()}; fails
 * when no branch matches and no default is configured.
 */
public final class GatewayHandler implements NodeHandler<GatewayConfig, Object, Object> {

    @Override
    public String type() {
        return NodeTypes.GATEWAY;
    }

    @Override
    public Class<GatewayConfig> configType() {
        return GatewayConfig.class;
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, GatewayConfig config) {
        if (config != null && config.branches() != null) {
            for (GatewayConfig.Branch branch : config.branches()) {
                if (branch == null) continue;
                // First-match wins: branches are evaluated in declaration order.
                if (ConditionEvaluator.evaluate(branch.when(), ctx)) {
                    if (branch.outcome() == null || branch.outcome().isBlank()) {
                        return NodeResult.failed("matched branch has blank outcome");
                    }
                    return NodeResult.completed(branch.outcome());
                }
            }
        }
        // Default outcome is a catch-all rather than an explicit branch so it doesn't have a
        // condition; it fires when no condition-based branch matched.
        if (config != null
                && config.defaultOutcome() != null
                && !config.defaultOutcome().isBlank()) {
            return NodeResult.completed(config.defaultOutcome());
        }
        return NodeResult.failed("no branch matched");
    }
}
