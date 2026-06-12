package org.carl.infrastructure.workflow.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.ConditionEvaluator;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ConditionEvaluatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void literalsAreCaseInsensitive() {
        TestContext ctx = new TestContext();
        assertTrue(ConditionEvaluator.evaluate("true", ctx));
        assertTrue(ConditionEvaluator.evaluate("TRUE", ctx));
        assertFalse(ConditionEvaluator.evaluate("false", ctx));
        assertFalse(ConditionEvaluator.evaluate("FALSE", ctx));
    }

    @Test
    void nullOrBlankIsFalse() {
        assertFalse(ConditionEvaluator.evaluate(null, new TestContext()));
        assertFalse(ConditionEvaluator.evaluate("", new TestContext()));
        assertFalse(ConditionEvaluator.evaluate("   ", new TestContext()));
    }

    @Test
    void variableLookupBooleanAndString() {
        TestContext ctx =
                new TestContext()
                        .setVariable("a", Boolean.TRUE)
                        .setVariable("b", "true")
                        .setVariable("c", "false")
                        .setVariable("d", Boolean.FALSE);
        assertTrue(ConditionEvaluator.evaluate("${a}", ctx));
        assertTrue(ConditionEvaluator.evaluate("${b}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${c}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${d}", ctx));
    }

    @Test
    void missingVariableIsFalse() {
        assertFalse(ConditionEvaluator.evaluate("${nope}", new TestContext()));
    }

    @Test
    void arbitraryStringIsFalse() {
        assertFalse(ConditionEvaluator.evaluate("1 + 1 == 2", new TestContext()));
        assertFalse(ConditionEvaluator.evaluate("yes", new TestContext()));
    }

    @Test
    void nullCtxOrEmptyExpressionGuard() {
        assertFalse(ConditionEvaluator.evaluate("${a}", null));
        assertFalse(ConditionEvaluator.evaluate("${}", new TestContext()));
    }

    // -----------------------------------------------------------------------
    // variables.fieldName comparisons
    // -----------------------------------------------------------------------

    @Test
    void variablesNumericGreaterThan() {
        TestContext ctx = new TestContext().setVariable("amount", 15000);
        assertTrue(ConditionEvaluator.evaluate("${variables.amount > 10000}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.amount > 20000}", ctx));
    }

    @Test
    void variablesNumericGreaterThanOrEqual() {
        TestContext ctx = new TestContext().setVariable("amount", 10000);
        assertTrue(ConditionEvaluator.evaluate("${variables.amount >= 10000}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.amount >= 10001}", ctx));
    }

    @Test
    void variablesNumericLessThan() {
        TestContext ctx = new TestContext().setVariable("count", 3);
        assertTrue(ConditionEvaluator.evaluate("${variables.count < 5}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.count < 3}", ctx));
    }

    @Test
    void variablesStringEquality() {
        TestContext ctx = new TestContext().setVariable("status", "approved");
        assertTrue(ConditionEvaluator.evaluate("${variables.status == \"approved\"}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.status == \"rejected\"}", ctx));
    }

    @Test
    void variablesStringNotEqual() {
        TestContext ctx = new TestContext().setVariable("status", "pending");
        assertTrue(ConditionEvaluator.evaluate("${variables.status != \"approved\"}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${variables.status != \"pending\"}", ctx));
    }

    @Test
    void variablesSingleQuotedString() {
        TestContext ctx = new TestContext().setVariable("role", "admin");
        assertTrue(ConditionEvaluator.evaluate("${variables.role == 'admin'}", ctx));
    }

    // -----------------------------------------------------------------------
    // businessData path navigation
    // -----------------------------------------------------------------------

    @Test
    void businessDataNumericComparison() throws Exception {
        TestContext ctx = new TestContext().setBusinessData(MAPPER.readTree("{\"days\": 5}"));
        assertTrue(ConditionEvaluator.evaluate("${businessData.days >= 5}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${businessData.days >= 6}", ctx));
        assertTrue(ConditionEvaluator.evaluate("${businessData.days == 5}", ctx));
    }

    @Test
    void businessDataNestedPath() throws Exception {
        TestContext ctx =
                new TestContext().setBusinessData(MAPPER.readTree("{\"applicant\":{\"level\":3}}"));
        assertTrue(ConditionEvaluator.evaluate("${businessData.applicant.level > 2}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${businessData.applicant.level > 3}", ctx));
    }

    @Test
    void businessDataBooleanField() throws Exception {
        TestContext ctx = new TestContext().setBusinessData(MAPPER.readTree("{\"urgent\": true}"));
        assertTrue(ConditionEvaluator.evaluate("${businessData.urgent == true}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${businessData.urgent == false}", ctx));
    }

    @Test
    void businessDataMissingFieldReturnsFalse() throws Exception {
        TestContext ctx = new TestContext().setBusinessData(MAPPER.readTree("{\"days\": 3}"));
        assertFalse(ConditionEvaluator.evaluate("${businessData.amount > 0}", ctx));
    }

    // -----------------------------------------------------------------------
    // results.nodeId.* path resolution
    // -----------------------------------------------------------------------

    @Test
    void resultsOutcomeEquality() {
        TestContext ctx =
                new TestContext().setResult("createOrder", NodeResult.completed("SUCCESS"));
        assertTrue(
                ConditionEvaluator.evaluate("${results.createOrder.outcome == \"SUCCESS\"}", ctx));
        assertFalse(
                ConditionEvaluator.evaluate("${results.createOrder.outcome == \"FAILED\"}", ctx));
    }

    @Test
    void resultsStatusEquality() {
        TestContext ctx =
                new TestContext().setResult("myNode", NodeResult.completed("SUCCESS"));
        assertTrue(ConditionEvaluator.evaluate("${results.myNode.status == \"COMPLETED\"}", ctx));
    }

    @Test
    void resultsPayloadFieldDirect() {
        TestContext ctx =
                new TestContext()
                        .setResult(
                                "createOrder",
                                NodeResult.completed(
                                        "SUCCESS", Map.of("orderId", "ORD-42")));
        // payload field without "payload." prefix
        Object val = ctx.resultOf("createOrder").payload().get("orderId");
        assertTrue("ORD-42".equals(val));
        // expression: results.createOrder.orderId
        assertTrue(
                ConditionEvaluator.evaluate("${results.createOrder.orderId == \"ORD-42\"}", ctx));
    }

    @Test
    void resultsPayloadFieldWithPrefix() {
        TestContext ctx =
                new TestContext()
                        .setResult(
                                "createOrder",
                                NodeResult.completed(
                                        "SUCCESS", Map.of("orderId", "ORD-42")));
        // expression: results.createOrder.payload.orderId
        assertTrue(
                ConditionEvaluator.evaluate(
                        "${results.createOrder.payload.orderId == \"ORD-42\"}", ctx));
    }

    @Test
    void resultsMissingNodeReturnsFalse() {
        TestContext ctx = new TestContext();
        assertFalse(
                ConditionEvaluator.evaluate("${results.nonExistent.outcome == \"SUCCESS\"}", ctx));
    }

    // -----------------------------------------------------------------------
    // Backward compat: bare ${varName} (no dot) reads from variables
    // -----------------------------------------------------------------------

    @Test
    void backwardCompatBareVarName() {
        TestContext ctx = new TestContext().setVariable("approved", Boolean.TRUE);
        assertTrue(ConditionEvaluator.evaluate("${approved}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${notSet}", ctx));
    }

    // -----------------------------------------------------------------------
    // evaluateOrTrue
    // -----------------------------------------------------------------------

    @Test
    void evaluateOrTrueNullOrBlankIsTrue() {
        assertTrue(ConditionEvaluator.evaluateOrTrue(null, new TestContext()));
        assertTrue(ConditionEvaluator.evaluateOrTrue("", new TestContext()));
        assertTrue(ConditionEvaluator.evaluateOrTrue("  ", new TestContext()));
    }

    @Test
    void evaluateOrTrueWithExpressionWorks() {
        TestContext ctx = new TestContext().setVariable("x", 5);
        assertTrue(ConditionEvaluator.evaluateOrTrue("${variables.x > 3}", ctx));
        assertFalse(ConditionEvaluator.evaluateOrTrue("${variables.x > 10}", ctx));
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    void numericLiteralsComparedDirectly() {
        TestContext ctx = new TestContext();
        assertTrue(ConditionEvaluator.evaluate("${5 > 3}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${3 > 5}", ctx));
        assertTrue(ConditionEvaluator.evaluate("${5 == 5}", ctx));
        assertTrue(ConditionEvaluator.evaluate("${5 != 6}", ctx));
    }

    @Test
    void quotedStringsWithOperatorInsideAreNotSplitOnOperator() {
        TestContext ctx = new TestContext().setVariable("label", "a >= b");
        // The operator inside the quoted rhs should not be split
        assertTrue(ConditionEvaluator.evaluate("${variables.label == \"a >= b\"}", ctx));
    }

    @Test
    void nullLiteralComparison() {
        TestContext ctx = new TestContext();
        // null == null is true
        assertTrue(ConditionEvaluator.evaluate("${null == null}", ctx));
        assertFalse(ConditionEvaluator.evaluate("${null != null}", ctx));
    }

    // -----------------------------------------------------------------------
    // Nested payload navigation: results.nodeId.payload.output.field
    // -----------------------------------------------------------------------

    @Test
    void resultsNestedJsonNodePayloadStringField() throws Exception {
        // ServiceTaskHandler stores the activity output as Map.of("output", outputNode)
        // where outputNode is a JsonNode. Conditions must navigate into it.
        JsonNode outputNode = MAPPER.readTree("{\"orderId\": \"ORD-001\", \"amount\": 500}");
        TestContext ctx =
                new TestContext()
                        .setResult(
                                "createOrder",
                                NodeResult.completed(
                                        "SUCCESS", Map.of("output", outputNode)));
        // ${results.createOrder.payload.output.orderId} == "ORD-001"
        assertTrue(
                ConditionEvaluator.evaluate(
                        "${results.createOrder.payload.output.orderId == \"ORD-001\"}", ctx));
        assertFalse(
                ConditionEvaluator.evaluate(
                        "${results.createOrder.payload.output.orderId == \"ORD-999\"}", ctx));
    }

    @Test
    void resultsNestedJsonNodePayloadNumericComparison() throws Exception {
        JsonNode outputNode = MAPPER.readTree("{\"orderId\": \"ORD-001\", \"amount\": 500}");
        TestContext ctx =
                new TestContext()
                        .setResult(
                                "createOrder",
                                NodeResult.completed(
                                        "SUCCESS", Map.of("output", outputNode)));
        // ${results.createOrder.payload.output.amount > 400} is true; > 600 is false
        assertTrue(
                ConditionEvaluator.evaluate(
                        "${results.createOrder.payload.output.amount > 400}", ctx));
        assertFalse(
                ConditionEvaluator.evaluate(
                        "${results.createOrder.payload.output.amount > 600}", ctx));
    }

    @Test
    void businessDataMultiLevelNestedPath() throws Exception {
        TestContext ctx =
                new TestContext()
                        .setBusinessData(MAPPER.readTree("{\"address\": {\"city\": \"Beijing\"}}"));
        // ${businessData.address.city == "Beijing"} must be true
        assertTrue(ConditionEvaluator.evaluate("${businessData.address.city == \"Beijing\"}", ctx));
        assertFalse(
                ConditionEvaluator.evaluate("${businessData.address.city == \"Shanghai\"}", ctx));
    }
}
