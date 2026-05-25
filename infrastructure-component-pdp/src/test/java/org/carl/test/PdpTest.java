package org.carl.test;

import org.carl.infrastructure.pdp.Pdp;
import org.carl.infrastructure.pdp.PolicyDecision;
import org.carl.infrastructure.pdp.PolicyRequest;
import org.carl.infrastructure.pdp.impl.DefaultPdp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdpTest {

    private static final PolicyRequest REQUEST = new PolicyRequest("alice", "read", "document:1");

    @Test
    void permitWhenPolicyAllows() {
        Pdp pdp = new DefaultPdp(List.of(req -> PolicyDecision.PERMIT));
        assertEquals(PolicyDecision.PERMIT, pdp.evaluate(REQUEST));
    }

    @Test
    void denyWhenPolicyDenies() {
        Pdp pdp = new DefaultPdp(List.of(req -> PolicyDecision.DENY));
        assertEquals(PolicyDecision.DENY, pdp.evaluate(REQUEST));
    }

    @Test
    void defaultDenyWhenNoPoliciesRegistered() {
        Pdp pdp = new DefaultPdp(List.of());
        assertEquals(PolicyDecision.DENY, pdp.evaluate(REQUEST));
    }

    @Test
    void denyOverridesPermit() {
        Pdp pdp = new DefaultPdp(List.of(
                req -> PolicyDecision.PERMIT,
                req -> PolicyDecision.DENY
        ));
        assertEquals(PolicyDecision.DENY, pdp.evaluate(REQUEST));
    }

    @Test
    void delegatingDefaultMethodOnPdp() {
        Pdp pdp = new DefaultPdp(List.of(req -> PolicyDecision.PERMIT));
        assertEquals(PolicyDecision.PERMIT, pdp.evaluate("alice", "read", "document:1"));
    }
}
