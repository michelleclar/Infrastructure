package org.carl.infrastructure.pdp;

public interface Pdp {

    PolicyDecision evaluate(PolicyRequest request);

    default PolicyDecision evaluate(String subject, String action, String resource) {
        return evaluate(new PolicyRequest(subject, action, resource));
    }
}
