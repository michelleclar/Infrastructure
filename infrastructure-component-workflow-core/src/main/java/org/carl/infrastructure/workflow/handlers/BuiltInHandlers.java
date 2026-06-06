package org.carl.infrastructure.workflow.handlers;

import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;

import java.util.Objects;

/**
 * Convenience bootstrap registering every built-in {@code NodeHandler} into a {@link
 * NodeHandlerRegistry}.
 *
 * <p>The order is fixed for predictability but does not affect runtime semantics; the registry is
 * keyed by {@link org.carl.infrastructure.workflow.spi.NodeHandler#type()}.
 */
public final class BuiltInHandlers {

    /**
     * Register all nine built-in handlers ({@code serviceTask}, {@code approvalTask}, {@code
     * userTask}, {@code eventTask}, {@code timerTask}, {@code taskGroup}, {@code gateway}, {@code
     * subProcess}, {@code endTask}) into the provided registry.
     *
     * @throws IllegalStateException if any of the built-in types is already registered.
     */
    public static void registerAll(NodeHandlerRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        registry.registerBuiltIn(new ServiceTaskHandler());
        registry.registerBuiltIn(new ApprovalTaskHandler());
        registry.registerBuiltIn(new UserTaskHandler());
        registry.registerBuiltIn(new EventTaskHandler());
        registry.registerBuiltIn(new TimerTaskHandler());
        registry.registerBuiltIn(new TaskGroupHandler());
        registry.registerBuiltIn(new GatewayHandler());
        registry.registerBuiltIn(new SubProcessHandler());
        registry.registerBuiltIn(new EndTaskHandler());
    }

    private BuiltInHandlers() {
        throw new AssertionError("no instances");
    }
}
