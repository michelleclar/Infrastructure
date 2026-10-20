package org.carl.infra.ruleengine;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shares executors by options value equality. The process-lifetime cache is unbounded:
 * use only a finite set of application-owned configurations, never arbitrary public input.
 * Each executor also enables QLExpress's unbounded expression cache.
 */
public final class ExpressionFactory {
    private static final ConcurrentHashMap<ExpressionOptions, ExpressionExecutor> EXECUTORS = new ConcurrentHashMap<>();

    private ExpressionFactory() {
    }

    public static ExpressionExecutor getDefault() {
        return get(ExpressionOptions.DEFAULT);
    }

    public static ExpressionExecutor get(ExpressionOptions options) {
        return EXECUTORS.computeIfAbsent(Objects.requireNonNull(options, "options"), ExpressionExecutor::new);
    }
}
