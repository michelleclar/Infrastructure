package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.carl.infrastructure.workflow.spi.ConditionEvaluator;

import java.util.List;

/**
 * Configuration for {@link GatewayHandler}.
 *
 * <p>Branches are evaluated in order; the first branch whose {@code when} expression evaluates to
 * {@code true} wins. If none match, {@code defaultOutcome} is emitted; if it too is absent, the
 * gateway fails.
 *
 * @param branches ordered list of branch condition-to-outcome mappings; {@code null} is treated as
 *     empty.
 * @param defaultOutcome fallback outcome when no branch matches; nullable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatewayConfig(List<Branch> branches, String defaultOutcome) {

    /**
     * A conditional branch.
     *
     * @param when EL guard expression evaluated by {@link ConditionEvaluator}.
     * @param outcome outcome emitted when the branch condition is satisfied.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Branch(String when, String outcome) {}
}
