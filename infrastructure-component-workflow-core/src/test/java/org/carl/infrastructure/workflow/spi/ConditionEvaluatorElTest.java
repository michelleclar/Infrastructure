package org.carl.infrastructure.workflow.spi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Coverage for the Jakarta EL-backed evaluator: {@code ${ctx....}} top-level view, native EL
 * operators ({@code &&}, {@code empty}), and deep navigation across {@link JsonNode} and {@code
 * payload.output.*}.
 */
class ConditionEvaluatorElTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void ctxBusinessDataNumericComparison() throws Exception {
        TestCtx ctx = new TestCtx().setBusinessData(MAPPER.readTree("{\"amount\":2000}"));
        assertTrue(ConditionEvaluator.evaluate("${ctx.businessData.amount > 1000}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${ctx.businessData.amount > 3000}", ctx));
    }

    @Test
    void ctxVariablesEqualityViaSingleQuotedLiteral() {
        TestCtx ctx = new TestCtx().setVariable("role", "admin");
        assertTrue(ConditionEvaluator.evaluate("${ctx.variables.role == 'admin'}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${ctx.variables.role == 'user'}", ctx));
    }

    @Test
    void ctxResultsBracketAccessOutcome() {
        TestCtx ctx = new TestCtx().setResult("hrApproval", NodeResult.completed("APPROVED"));
        assertTrue(
                ConditionEvaluator.evaluate(
                        "${ctx.results['hrApproval'].outcome == 'APPROVED'}", ctx));
        assertFalse(
                ConditionEvaluator.evaluate(
                        "${ctx.results['hrApproval'].outcome == 'REJECTED'}", ctx));
    }

    @Test
    void elNativeAndOperator() {
        TestCtx ctx = new TestCtx().setVariable("x", 7).setVariable("y", 3);
        assertTrue(ConditionEvaluator.evaluate("${variables.x > 5 && variables.y < 10}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.x > 5 && variables.y > 10}", ctx));
    }

    @Test
    void elEmptyOperatorOnMissingKey() {
        TestCtx ctx = new TestCtx().setVariable("present", "value");
        assertTrue(ConditionEvaluator.evaluate("${empty variables.missingKey}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${empty variables.present}", ctx));
    }

    @Test
    void businessDataDeepJsonNodePath() throws Exception {
        TestCtx ctx =
                new TestCtx().setBusinessData(MAPPER.readTree("{\"user\":{\"role\":\"admin\"}}"));
        assertTrue(ConditionEvaluator.evaluate("${businessData.user.role == 'admin'}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${businessData.user.role == 'guest'}", ctx));
    }

    @Test
    void resultsPayloadNumericGreaterEqual() {
        TestCtx ctx =
                new TestCtx().setResult("x", NodeResult.completed("OK", Map.of("balance", 500)));
        assertTrue(ConditionEvaluator.evaluate("${results.x.payload.balance >= 500}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${results.x.payload.balance >= 501}", ctx));
    }

    // -------- minimal NodeExecutionContext impl -------------------------------

    private static final class TestCtx implements NodeExecutionContext {
        private final Map<String, NodeResult> results = new HashMap<>();
        private final Map<String, Object> variables = new LinkedHashMap<>();
        private JsonNode businessData;

        TestCtx setResult(String id, NodeResult r) {
            results.put(id, r);
            return this;
        }

        TestCtx setVariable(String k, Object v) {
            variables.put(k, v);
            return this;
        }

        TestCtx setBusinessData(JsonNode n) {
            this.businessData = n;
            return this;
        }

        @Override
        public String workflowId() {
            return "wf";
        }

        @Override
        public String instanceId() {
            return "inst";
        }

        @Override
        public String currentNodeId() {
            return "node";
        }

        @Override
        public JsonNode businessData() {
            return businessData;
        }

        @Override
        public Map<String, Object> variables() {
            return variables;
        }

        @Override
        public NodeResult resultOf(String nodeId) {
            return results.get(nodeId);
        }

        @Override
        public WorkflowEvent currentEvent() {
            return null;
        }
    }
}
