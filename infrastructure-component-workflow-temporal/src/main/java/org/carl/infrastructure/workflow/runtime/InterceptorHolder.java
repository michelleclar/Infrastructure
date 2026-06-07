package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;

/**
 * Static holder exposing the {@link WorkflowInterceptorRegistry} to workflow code.
 *
 * <p>Temporal workflow code cannot capture worker-scope objects directly; the indirection through
 * this volatile static field is the canonical workaround (same pattern as {@link HandlerHolder}).
 *
 * <p>The default value is an empty (non-null) registry so workflow code never needs a null check.
 * When no interceptors are registered the registry's lists are empty and the fast-path in {@link
 * GenericWorkflowImpl} skips all hook dispatch with zero overhead (R6).
 */
public final class InterceptorHolder {

    /** Default empty registry — never null, never mutated when no interceptors are registered. */
    private static volatile WorkflowInterceptorRegistry REGISTRY =
            new WorkflowInterceptorRegistry();

    private InterceptorHolder() {
        throw new AssertionError("no instances");
    }

    /**
     * Install a custom registry. Called by {@link WorkerSetup#setup} when the caller provides an
     * {@link WorkflowInterceptorRegistry}.
     */
    public static void install(WorkflowInterceptorRegistry registry) {
        if (registry == null) {
            return; // keep the default empty registry
        }
        REGISTRY = registry;
    }

    /**
     * Returns the installed registry. Always non-null; returns an empty registry when none was
     * installed.
     */
    public static WorkflowInterceptorRegistry registry() {
        return REGISTRY;
    }
}
