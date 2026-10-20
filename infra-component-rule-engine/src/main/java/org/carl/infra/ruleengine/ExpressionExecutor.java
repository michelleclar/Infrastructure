package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable concrete wrapper with default isolation and per-call shallow context copies.
 * QLExpress syntax and runtime exceptions propagate unchanged; this is not a
 * vendor-independent scripting language.
 */
public final class ExpressionExecutor {
    private final Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    private final QLOptions options;

    public ExpressionExecutor() {
        this(ExpressionOptions.DEFAULT);
    }

    public ExpressionExecutor(long timeoutMillis) {
        this(new ExpressionOptions(timeoutMillis, false));
    }

    ExpressionExecutor(ExpressionOptions expressionOptions) {
        Objects.requireNonNull(expressionOptions, "options");
        options = QLOptions.builder().cache(true).timeoutMillis(expressionOptions.timeoutMillis())
                .precise(expressionOptions.precise()).polluteUserContext(false).build();
    }

    /** Checks syntax with the execution runner; does not execute or validate business data. */
    public void check(String script) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        runner.check(script);
    }

    /**
     * Executes without an automatic check. Does not make nested objects read-only.
     * Callers must keep shared nested objects stable; runtime QLExpress errors propagate unchanged.
     */
    public Object execute(String script, Map<String, ?> facts) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return runner.execute(script, new HashMap<>(Objects.requireNonNull(facts, "facts")), options).getResult();
    }

    /** Executes a compiled condition, rejecting reserved fact names and parameter collisions. */
    public Object execute(ConditionCompiler.CompiledCondition condition, Map<String, ?> facts) {
        Objects.requireNonNull(condition, "condition");
        Map<String, Object> context = new HashMap<>(Objects.requireNonNull(facts, "facts"));
        for (String key : context.keySet()) {
            if (key != null && key.startsWith(ConditionCompiler.PARAMETER_PREFIX)) {
                throw new IllegalArgumentException("facts use reserved parameter namespace: " + key);
            }
        }
        for (Map.Entry<String, Object> parameter : condition.parameters().entrySet()) {
            if (context.containsKey(parameter.getKey())) {
                throw new IllegalArgumentException("parameter collides with fact: " + parameter.getKey());
            }
            context.put(parameter.getKey(), parameter.getValue());
        }
        return execute(condition.script(), context);
    }
}
