package org.carl.infrastructure.workflow.runtime;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity bridge for {@link org.carl.infrastructure.workflow.interceptor.AsyncInterceptor}
 * hooks. {@link GenericWorkflowImpl} dispatches an {@link AsyncHookInvocation} here via
 * {@link io.temporal.workflow.Async#procedure} so async interceptors can perform I/O safely
 * outside the deterministic workflow thread.
 */
@ActivityInterface
public interface AsyncInterceptorActivity {

    /**
     * Invoked once per lifecycle hook. The activity implementation iterates over all registered
     * {@link org.carl.infrastructure.workflow.interceptor.AsyncInterceptor} instances and dispatches
     * to the appropriate hook method based on {@link AsyncHookInvocation#phase()}.
     */
    @ActivityMethod
    void invoke(AsyncHookInvocation invocation);
}
