package org.carl.infrastructure.pdp;

import org.carl.infrastructure.pdp.impl.DefaultPdp;

import java.util.List;

public interface IPdpAbility {

    default Pdp getPdp() {
        return new DefaultPdp(List.of());
    }

    default PolicyDecision evaluate(PolicyRequest request) {
        return getPdp().evaluate(request);
    }
}
