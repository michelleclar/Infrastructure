package org.carl.infrastructure.workflow.build;

import org.carl.infrastructure.statemachine.ActionWrapper;

import java.util.function.BiConsumer;

public class ActionWrapperFactory {
    public static <S, E, C> ActionWrapper<S, E, C> of(String name) {
        return next ->
                (from, to, event, ctx) -> {
                    long start = System.currentTimeMillis();
                    try {
                        System.out.printf("▶️ [%s] %s -> %s by %s\n", name, from, to, event);
                        next.execute(from, to, event, ctx);
                        System.out.printf(
                                "✅ [%s] completed in %dms\n",
                                name, System.currentTimeMillis() - start);
                    } catch (Exception e) {
                        System.err.printf("❌ [%s] error: %s\n", name, e.getMessage());
                        throw e;
                    }
                };
    }

    public static <S, E, C> ActionWrapper<S, E, C> of(BiConsumer<C, Exception> auditHandler) {
        return next ->
                (from, to, event, ctx) -> {
                    Exception ex = null;
                    try {
                        next.execute(from, to, event, ctx);
                    } catch (Exception e) {
                        ex = e;
                        throw e;
                    } finally {
                        auditHandler.accept(ctx, ex);
                    }
                };
    }

    public static <S, E, C> ActionWrapper<S, E, C> persistence(
            String workflowId, String entityId, String bizType) {
        return next -> (from, to, event, ctx) -> {};
    }
}
