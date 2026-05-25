package org.carl.infrastructure.pdp;

public interface IPdpAbility {

    Pdp getPdp();

    default PolicyDecision evaluate(PolicyRequest request) {
        return getPdp().evaluate(request);
    }
}
