package org.carl.infrastructure.pdp;

@FunctionalInterface
public interface Policy {
    PolicyDecision evaluate(PolicyRequest request);
}
