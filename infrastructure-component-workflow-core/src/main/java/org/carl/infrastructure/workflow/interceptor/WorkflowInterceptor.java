package org.carl.infrastructure.workflow.interceptor;

/**
 * Base marker for workflow interceptors. Use either {@link DeterministicInterceptor} (in-workflow,
 * must be pure) or {@link AsyncInterceptor} (dispatched to activity, IO-safe).
 *
 * <p>Implementations are picked up by {@link WorkflowInterceptorRegistry} and invoked by the
 * runtime at well-defined lifecycle points: workflow start/end, node enter/exit/error, event
 * arrival, compensation.
 */
public interface WorkflowInterceptor {
    /** Lower order runs first. Default 0. */
    default int order() {
        return 0;
    }
}
