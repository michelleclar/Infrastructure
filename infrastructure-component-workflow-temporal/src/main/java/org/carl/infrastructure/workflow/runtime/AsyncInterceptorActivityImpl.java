package org.carl.infrastructure.workflow.runtime;

import org.carl.infrastructure.workflow.interceptor.AsyncInterceptor;
import org.carl.infrastructure.workflow.interceptor.InterceptorContext;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Default {@link AsyncInterceptorActivity} implementation.
 *
 * <p>Receives an {@link AsyncHookInvocation} dispatched by {@link GenericWorkflowImpl}, builds an
 * {@link InterceptorContext} from the invocation fields, and calls the appropriate hook method on
 * every registered {@link AsyncInterceptor}. Exceptions from individual interceptors are caught,
 * logged to stderr (best-effort), and do not abort the remaining interceptors or the activity.
 */
public final class AsyncInterceptorActivityImpl implements AsyncInterceptorActivity {

    private final WorkflowInterceptorRegistry registry;

    public AsyncInterceptorActivityImpl(WorkflowInterceptorRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void invoke(AsyncHookInvocation inv) {
        if (inv == null) {
            return;
        }
        InterceptorContext ctx =
                new SimpleInterceptorContext(
                        inv.workflowId(),
                        inv.instanceId(),
                        inv.definitionId(),
                        inv.businessData());

        List<AsyncInterceptor> interceptors = registry.async();
        for (AsyncInterceptor i : interceptors) {
            try {
                switch (inv.phase()) {
                    case HookPhases.WORKFLOW_START -> i.onWorkflowStart(ctx);
                    case HookPhases.WORKFLOW_END   -> i.onWorkflowEnd(ctx, inv.nodeResult());
                    case HookPhases.NODE_ENTER     -> i.onNodeEnter(ctx, inv.node());
                    case HookPhases.NODE_EXIT      -> i.onNodeExit(ctx, inv.node(), inv.nodeResult());
                    case HookPhases.NODE_ERROR     -> i.onNodeError(ctx, inv.node(), inv.errorMessage());
                    case HookPhases.EVENT          -> i.onEvent(ctx, inv.event());
                    case HookPhases.COMPENSATE     -> i.onCompensate(ctx, inv.node(), inv.nodeResult());
                    default ->
                            System.err.println(
                                    "[AsyncInterceptorActivity] unknown phase: " + inv.phase());
                }
            } catch (RuntimeException e) {
                // best-effort: log + continue to next interceptor
                System.err.println(
                        "[AsyncInterceptor] "
                                + i.getClass().getSimpleName()
                                + " threw on "
                                + inv.phase()
                                + ": "
                                + e.getMessage());
            }
        }
    }
}
