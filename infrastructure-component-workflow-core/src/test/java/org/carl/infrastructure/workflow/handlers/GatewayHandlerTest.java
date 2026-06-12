package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

class GatewayHandlerTest {

    private final GatewayHandler handler = new GatewayHandler();

    @Test
    void metadataMatchesSpec() {
        assertEquals(NodeTypes.GATEWAY, handler.type());
        assertEquals(GatewayConfig.class, handler.configType());
    }

    @Test
    void trueLiteralBranchWins() {
        GatewayConfig cfg =
                new GatewayConfig(
                        List.of(
                                new GatewayConfig.Branch("true", "FAST"),
                                new GatewayConfig.Branch("true", "SLOW")),
                        "DEFAULT");
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.COMPLETED, r.status());
        assertEquals("FAST", r.outcome());
    }

    @Test
    void variableExpressionLookup() {
        GatewayConfig cfg =
                new GatewayConfig(
                        List.of(new GatewayConfig.Branch("${flag}", "MATCHED")), "FALLBACK");
        TestContext ctx = new TestContext().setVariable("flag", Boolean.TRUE);
        assertEquals("MATCHED", handler.run(ctx, cfg).outcome());
    }

    @Test
    void falseLiteralFallsBackToDefault() {
        GatewayConfig cfg =
                new GatewayConfig(List.of(new GatewayConfig.Branch("false", "X")), "DEFAULT");
        assertEquals("DEFAULT", handler.run(new TestContext(), cfg).outcome());
    }

    @Test
    void noBranchAndNoDefaultFails() {
        GatewayConfig cfg =
                new GatewayConfig(List.of(new GatewayConfig.Branch("false", "X")), null);
        NodeResult r = handler.run(new TestContext(), cfg);
        assertEquals(NodeStatus.FAILED, r.status());
    }

    @Test
    void emptyBranchesGoToDefault() {
        GatewayConfig cfg = new GatewayConfig(List.of(), "ONLY");
        assertEquals("ONLY", handler.run(new TestContext(), cfg).outcome());
    }

    @Test
    void conditionEvaluatorRejectsArbitraryString() {
        // "${missing}" → variable absent → false; should reach default.
        GatewayConfig cfg =
                new GatewayConfig(List.of(new GatewayConfig.Branch("${missing}", "X")), "DEFAULT");
        assertEquals("DEFAULT", handler.run(new TestContext(), cfg).outcome());
    }

    @Test
    void conditionEvaluatorAcceptsStringBoolean() {
        // variable value as a string "true" is also accepted.
        GatewayConfig cfg =
                new GatewayConfig(List.of(new GatewayConfig.Branch("${flag}", "HIT")), "DEFAULT");
        TestContext ctx = new TestContext().setVariable("flag", "true");
        assertEquals("HIT", handler.run(ctx, cfg).outcome());
    }
}
