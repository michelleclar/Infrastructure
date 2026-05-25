package org.carl.infrastructure.pdp.impl;

import org.carl.infrastructure.pdp.Pdp;
import org.carl.infrastructure.pdp.Policy;
import org.carl.infrastructure.pdp.PolicyDecision;
import org.carl.infrastructure.pdp.PolicyRequest;

import java.util.List;

public class DefaultPdp implements Pdp {

    private final List<Policy> policies;

    public DefaultPdp(List<Policy> policies) {
        this.policies = List.copyOf(policies);
    }

    @Override
    public PolicyDecision evaluate(PolicyRequest request) {
        boolean anyPermit = false;

        for (Policy policy : policies) {
            PolicyDecision decision = policy.evaluate(request);
            if (decision == PolicyDecision.DENY) {
                return PolicyDecision.DENY;
            }
            if (decision == PolicyDecision.PERMIT) {
                anyPermit = true;
            }
        }

        return anyPermit ? PolicyDecision.PERMIT : PolicyDecision.DENY;
    }
}
