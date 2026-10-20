package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.exception.QLException;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.alibaba.qlexpress4.exception.QLTimeoutException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SlimApiTest {
    private final ConditionCompiler compiler = new ConditionCompiler();
    private final QLExpressExecutor executor = new QLExpressExecutor();

    private static Map<String, Object> leaf(String field, String operator, Object value) {
        Map<String, Object> leaf = new HashMap<>();
        leaf.put("field", field);
        leaf.put("operator", operator);
        leaf.put("value", value);
        return leaf;
    }

    private static Map<String, Object> group(String combinator, Object... rules) {
        return Map.of("combinator", combinator, "rules", List.of(rules));
    }

    @Test
    void nestingAndEveryOperator() {
        for (String op : List.of("==", "!=", ">", ">=", "<", "<=", "in")) {
            Object value = switch (op) {
                case "!=", ">" -> 2;
                case "<" -> 4;
                case "in" -> List.of(2, 3);
                default -> 3;
            };
            var compiled = compiler.compile(group("and", leaf("user.level", "==", "VIP"),
                    group("or", leaf("amount", op, value), leaf("amount", "==", -1))));
            assertTrue(compiled.script().startsWith("(("));
            assertEquals(true, executor.execute(compiled, Map.of("user", Map.of("level", "VIP"), "amount", 3)), op);
            assertEquals(false, executor.execute(compiled, Map.of("user", Map.of("level", "NORMAL"), "amount", 3)), op);
        }
    }

    @Test
    void explicitNullAndMissingValueAreDifferent() {
        var compiled = compiler.compile(leaf("user.level", "==", null));
        assertTrue(compiled.parameters().containsKey("__condition_0"));
        assertNull(compiled.parameters().get("__condition_0"));
        assertEquals(true, executor.execute(compiled, Map.of("user", Map.of())));
        assertThrows(UnsupportedOperationException.class, () -> compiled.parameters().put("x", 1));
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(Map.of("field", "x", "operator", "==")));
    }

    @Test
    void rejectsMalformedAndAmbiguousTrees() {
        List<Map<String, ?>> invalid = List.of(
                group("and"), group("xor", leaf("x", "==", 1)),
                Map.of("combinator", "and", "rules", "not a list"), group("and", "not a map"),
                Map.of("field", "x", "operator", "==", "value", 1, "rules", List.of()),
                leaf("x", "contains", 1), leaf("x", "=", 1), Map.of());
        for (var tree : invalid) {
            assertThrows(IllegalArgumentException.class, () -> compiler.compile(tree), tree.toString());
        }
        Map<String, Object> deep = leaf("x", "==", 1);
        for (int i = 0; i < 32; i++) {
            deep = group("and", deep);
        }
        var tooDeep = deep;
        assertThrows(IllegalArgumentException.class, () -> compiler.compile(tooDeep));
    }

    @Test
    void pathsAndValuesCannotInjectScript() {
        for (String path : List.of("", "a..b", "a[0]", "a.b()", "a || true", "a.'b'", "a; x=1", "__condition_0.x")) {
            assertThrows(IllegalArgumentException.class, () -> compiler.compile(leaf(path, "==", 1)), path);
        }
        assertThrows(QLSyntaxException.class, () -> compiler.compile(leaf("while", "==", 1)));
        String attack = "VIP') || true; stolen = 1; ('";
        var compiled = compiler.compile(leaf("user.level", "==", attack));
        assertFalse(compiled.script().contains(attack));
        assertEquals(false, executor.execute(compiled, Map.of("user", Map.of("level", "VIP"))));
        assertEquals(true, executor.execute(compiled, Map.of("user", Map.of("level", attack))));
    }

    @Test
    void rejectsNamespaceAndExplicitParameterCollisionsEvenWithNullFacts() {
        var compiled = compiler.compile(leaf("x", "==", 1));
        for (String key : List.of("__condition_0", "__condition_unused")) {
            Map<String, Object> facts = new HashMap<>();
            facts.put(key, null);
            assertThrows(IllegalArgumentException.class, () -> executor.execute(compiled, facts));
        }
        var custom = new ConditionCompiler.CompiledCondition("x == x", Map.of("x", 2));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(custom, Map.of("x", 1)));
    }

    @Test
    void validatorOnlyChecksSyntaxAndPreservesDetails() {
        ScriptValidator validator = new ScriptValidator();
        assertThrows(IllegalArgumentException.class, () -> validator.check(null));
        assertThrows(IllegalArgumentException.class, () -> validator.check(" \n"));
        QLSyntaxException syntax = assertThrows(QLSyntaxException.class, () -> validator.check("x > 0 &&\n(y > 1"));
        assertEquals(2, syntax.getLineNo());
        assertEquals("SYNTAX_ERROR", syntax.getErrorCode());
        assertTrue(syntax.getMessage().contains("Column"));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> validator.check("while (true) { 1 + 1 }"));
        validator.check("unknownFunction(missing)");
        assertThrows(QLException.class, () -> executor.execute("unknownFunction(missing)", Map.of()));
    }

    @Test
    void executionIsIsolatedTimedAndDoesNotWriteTopLevelFacts() {
        assertThrows(IllegalArgumentException.class, () -> new QLExpressExecutor(0));
        assertThrows(IllegalArgumentException.class, () -> new QLExpressExecutor(-1));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(" ", Map.of()));
        Map<String, Object> facts = new HashMap<>(Map.of("x", 1));
        assertEquals(3, executor.execute("x = 2; created = 3; created", facts));
        assertEquals(Map.of("x", 1), facts);
        assertThrows(QLException.class, () -> executor.execute("name.length()", Map.of("name", "abc")));
        assertThrows(QLTimeoutException.class, () -> new QLExpressExecutor(20).execute("while (true) { 1 + 1 }", Map.of()));
    }

    @Test
    void sharedInstancesKeepConcurrentCallsIndependent() throws Exception {
        var compiled = compiler.compile(leaf("x", ">=", 50));
        try (var pool = Executors.newFixedThreadPool(8)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 200; i++) {
                int value = i;
                futures.add(pool.submit(() -> {
                    Map<String, Object> facts = new HashMap<>(Map.of("x", value));
                    assertEquals(value >= 50, executor.execute(compiled, facts));
                    assertEquals(value + 1, executor.execute("local = x + 1; local", facts));
                    assertEquals(Map.of("x", value), facts);
                    assertEquals(true, executor.execute(compiler.compile(leaf("x", "==", value)), facts));
                }));
            }
            for (var future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }
    }
}
