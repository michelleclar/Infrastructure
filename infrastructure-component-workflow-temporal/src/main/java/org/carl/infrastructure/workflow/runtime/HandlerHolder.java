package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;

import java.util.Objects;

/**
 * Static holder exposing the {@link NodeHandlerRegistry} to workflow code.
 *
 * <p>Temporal workflow code cannot capture worker-scope objects directly; the indirection through
 * this volatile static field is the canonical workaround. {@link WorkerSetup#setup} writes here
 * before starting the worker.
 *
 * <p>Reads happen inside workflow code (deterministic — the registry never mutates after install).
 */
public final class HandlerHolder {

    private static volatile NodeHandlerRegistry REGISTRY;

    private HandlerHolder() {
        throw new AssertionError("no instances");
    }

    /** Install the registry used by all workflow executions running on this JVM. */
    public static void install(NodeHandlerRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        REGISTRY = registry;
    }

    public static NodeHandlerRegistry registry() {
        NodeHandlerRegistry r = REGISTRY;
        if (r == null) {
            throw new IllegalStateException(
                    "NodeHandlerRegistry not installed; call WorkerSetup.setup(...) before"
                            + " running workflows");
        }
        return r;
    }
}
