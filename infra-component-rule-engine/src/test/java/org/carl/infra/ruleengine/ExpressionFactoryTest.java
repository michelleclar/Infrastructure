package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.alibaba.qlexpress4.exception.QLTimeoutException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Public API contract of {@link ExpressionFactory} / {@link ExpressionOptions}: value-based
 * sharing, option validation, executor isolation and precision/timeout behavior.
 */
class ExpressionFactoryTest {

    @Test
    void defaultIsSameAsEqualOptionsOf1000MillisNonPrecise() {
        assertEquals(new ExpressionOptions(1000L, false), ExpressionOptions.DEFAULT);
        assertSame(ExpressionFactory.getDefault(), ExpressionFactory.get(new ExpressionOptions(1000L, false)));
        assertSame(ExpressionFactory.getDefault(), ExpressionFactory.getDefault());
    }

    @Test
    void equalOptionsShareOneExecutorWhileDifferentOptionsGetDistinctExecutors() {
        ExpressionExecutor precise = ExpressionFactory.get(new ExpressionOptions(700L, true));
        assertSame(precise, ExpressionFactory.get(new ExpressionOptions(700L, true)));
        assertNotSame(precise, ExpressionFactory.get(new ExpressionOptions(701L, true)),
                "different timeout must get a distinct executor");
        assertNotSame(precise, ExpressionFactory.get(new ExpressionOptions(700L, false)),
                "different precision must get a distinct executor");
        assertNotSame(precise, ExpressionFactory.getDefault());
        assertNotSame(ExpressionFactory.get(new ExpressionOptions(701L, true)),
                ExpressionFactory.get(new ExpressionOptions(700L, false)));
    }

    @Test
    void optionsIsAnImmutableRecordWithValueSemantics() {
        assertTrue(ExpressionOptions.class.isRecord());
        assertArrayEquals(new String[]{"timeoutMillis", "precise"},
                Arrays.stream(ExpressionOptions.class.getRecordComponents()).map(RecordComponent::getName).toArray(String[]::new),
                "record components must be timeoutMillis and precise");

        ExpressionOptions options = new ExpressionOptions(250L, true);
        assertEquals(250L, options.timeoutMillis());
        assertTrue(options.precise());
        assertEquals(new ExpressionOptions(250L, true), options);
        assertEquals(new ExpressionOptions(250L, true).hashCode(), options.hashCode());
        assertNotEquals(new ExpressionOptions(251L, true), options);
        assertNotEquals(new ExpressionOptions(250L, false), options);

        for (Field field : ExpressionOptions.class.getDeclaredFields()) {
            assertTrue(Modifier.isFinal(field.getModifiers()), "field must be final: " + field.getName());
        }
        for (Method method : ExpressionOptions.class.getDeclaredMethods()) {
            assertFalse(method.getName().startsWith("set"), "no mutator allowed: " + method.getName());
        }
    }

    @Test
    void rejectsNullOptionsAndNonPositiveTimeout() {
        for (long timeout : new long[]{0L, -1L, Long.MIN_VALUE}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new ExpressionOptions(timeout, false), "timeout " + timeout);
            assertTrue(e.getMessage().contains("timeoutMillis"), "message: " + e.getMessage());
        }
        assertEquals(1L, new ExpressionOptions(1L, false).timeoutMillis(), "smallest valid timeout is 1");

        NullPointerException npe = assertThrows(NullPointerException.class, () -> ExpressionFactory.get(null));
        assertTrue(npe.getMessage().contains("options"), "message: " + npe.getMessage());
    }

    @Test
    void parallelGetWithPreviouslyUnusedOptionsSharesOneInstanceAndKeepsFactsIsolated() throws Exception {
        ExpressionOptions fresh = new ExpressionOptions(31337L, false);
        int workers = 8;
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(workers)) {
            List<Future<ExpressionExecutor>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                int seed = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    ExpressionExecutor executor = ExpressionFactory.get(fresh);
                    Map<String, Object> facts = new HashMap<>(Map.of("x", seed));
                    assertEquals(seed + 1, executor.execute("x + 1", facts), "seed " + seed);
                    assertEquals(Map.of("x", seed), facts, "top-level facts must not be mutated for seed " + seed);
                    return executor;
                }));
            }
            start.countDown();
            ExpressionExecutor shared = futures.get(0).get(30, TimeUnit.SECONDS);
            for (Future<ExpressionExecutor> future : futures) {
                assertSame(shared, future.get(30, TimeUnit.SECONDS), "all parallel calls must share one instance");
            }
            assertSame(shared, ExpressionFactory.get(fresh), "later calls must reuse the same instance");
        }
    }

    @Test
    void executeCopiesTopLevelFactsAndKeepsMapsWithoutCrossTalk() {
        ExpressionExecutor executor = ExpressionFactory.getDefault();
        Map<String, Object> facts = new HashMap<>(Map.of("x", 1));
        assertEquals(3, executor.execute("x = 2; created = 3; created", facts));
        assertEquals(Map.of("x", 1), facts, "assignment must not leak into the caller's map");
        assertEquals(1, facts.size(), "no new top-level keys may appear");

        Map<String, Object> first = new HashMap<>(Map.of("x", 1));
        Map<String, Object> second = new HashMap<>(Map.of("x", 10));
        assertEquals(2, executor.execute("x + 1", first));
        assertEquals(11, executor.execute("x + 1", second));
        first.put("x", 100);
        assertEquals(101, executor.execute("x + 1", first));
        assertEquals(11, executor.execute("x + 1", second), "maps must stay independent across calls");
        assertEquals(Map.of("x", 10), second);
    }

    @Test
    void preciseOptionMakesDoubleInputAddUpToExactDecimal() {
        Map<String, Object> doubles = Map.of("a", 0.1, "b", 0.2);

        ExpressionExecutor precise = ExpressionFactory.get(new ExpressionOptions(1000L, true));
        assertEquals(true, precise.execute("a + b == 0.3", doubles), "precise: 0.1 + 0.2 must equal 0.3");
        Object sum = precise.execute("a + b", doubles);
        assertEquals(0, new BigDecimal("0.3").compareTo(new BigDecimal(String.valueOf(sum))),
                "precise sum must be 0.3 but was " + sum);

        ExpressionExecutor defaultExecutor = ExpressionFactory.getDefault();
        assertEquals(false, defaultExecutor.execute("a + b == 0.3", doubles), "default keeps double rounding error");
        double raw = ((Number) defaultExecutor.execute("a + b", doubles)).doubleValue();
        assertEquals(0.1 + 0.2, raw, 0.0);
        assertNotEquals(0.3, raw, 0.0);
    }

    @Test
    void checkOnFactoryInstanceRejectsBlankAndInvalidSyntax() {
        ExpressionExecutor executor = ExpressionFactory.getDefault();
        for (String blank : new String[]{null, "", "  \n\t"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> executor.check(blank), "script: " + blank);
            assertTrue(e.getMessage().contains("script"), "message: " + e.getMessage());
        }
        QLSyntaxException syntax = assertThrows(QLSyntaxException.class, () -> executor.check("x > 0 &&\n(y > 1"));
        assertEquals("SYNTAX_ERROR", syntax.getErrorCode());
        assertEquals(2, syntax.getLineNo());
    }

    @Test
    void tinyTimeoutAbortsLongRunningScriptButAllowsFastScripts() {
        ExpressionExecutor tiny = ExpressionFactory.get(new ExpressionOptions(20L, false));
        assertEquals(3, tiny.execute("1 + 2", Map.of()));
        QLTimeoutException timeout = assertThrows(QLTimeoutException.class,
                () -> tiny.execute("while (true) { 1 + 1 }", Map.of()));
        assertEquals("SCRIPT_TIME_OUT", timeout.getErrorCode());
    }

    @Test
    void conditionCompilerWithFactoryExecutorCompilesAndRunsTreeAndRejectsNullExecutor() {
        ExpressionExecutor precise = ExpressionFactory.get(new ExpressionOptions(1000L, true));
        ConditionCompiler compiler = new ConditionCompiler(precise);
        Map<String, Object> tree = Map.of("combinator", "and", "rules", List.of(
                Map.of("field", "user.level", "operator", "==", "value", "VIP"),
                Map.of("field", "amount", "operator", ">", "value", 100)));
        ConditionCompiler.CompiledCondition compiled = compiler.compile(tree);
        assertEquals(2, compiled.parameters().size());
        assertEquals(true, precise.execute(compiled, Map.of("user", Map.of("level", "VIP"), "amount", 150)));
        assertEquals(false, precise.execute(compiled, Map.of("user", Map.of("level", "VIP"), "amount", 100)),
                "boundary amount == 100 must not satisfy > 100");
        assertEquals(false, precise.execute(compiled, Map.of("user", Map.of("level", "BASIC"), "amount", 150)));

        NullPointerException npe = assertThrows(NullPointerException.class, () -> new ConditionCompiler(null));
        assertTrue(npe.getMessage().contains("executor"), "message: " + npe.getMessage());
    }
}
