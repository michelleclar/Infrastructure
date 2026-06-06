package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.ConditionEvaluator;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.Set;

/**
 * Built-in handler for {@code gateway} nodes.
 *
 * <p>Synchronous: evaluates branches in order using {@link ConditionEvaluator}; the first branch
 * whose condition is {@code true} wins. Falls back to {@link GatewayConfig#defaultOutcome()}; fails
 * when no branch matches and no default is configured.
 *
 * <p>The {@link #outcomes()} set is intentionally empty because the produced outcome string is
 * dynamic and depends on the configured branches.
 */
public final class GatewayHandler implements NodeHandler<GatewayConfig> {

    @Override
    public String type() {
        return NodeTypes.GATEWAY;
    }

    @Override
    public Class<GatewayConfig> configType() {
        return GatewayConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of();
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, GatewayConfig config) {
        if (config != null && config.branches() != null) {
            for (GatewayConfig.Branch branch : config.branches()) {
                if (branch == null) continue;
                if (ConditionEvaluator.evaluate(branch.when(), ctx)) {
                    if (branch.outcome() == null || branch.outcome().isBlank()) {
                        return NodeResult.failed("matched branch has blank outcome");
                    }
                    return NodeResult.completed(branch.outcome());
                }
            }
        }
        if (config != null
                && config.defaultOutcome() != null
                && !config.defaultOutcome().isBlank()) {
            return NodeResult.completed(config.defaultOutcome());
        }
        return NodeResult.failed("no branch matched");
    }
}
