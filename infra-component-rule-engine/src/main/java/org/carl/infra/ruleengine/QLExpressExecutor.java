package org.carl.infra.ruleengine;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Reusable runner with default isolation and per-call shallow context copies. */
public final class QLExpressExecutor {
    private final Express4Runner runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    private final QLOptions options;

    public QLExpressExecutor() {
        this(1000L);
    }

    public QLExpressExecutor(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        options = QLOptions.builder().cache(true).timeoutMillis(timeoutMillis)
                .polluteUserContext(false).build();
    }

    /** Does not make nested objects read-only. Runtime QLExpress errors propagate unchanged. */
    public Object execute(String script, Map<String, ?> facts) {
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("script must not be blank");
        }
        return runner.execute(script, new HashMap<>(Objects.requireNonNull(facts, "facts")), options).getResult();
    }

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
