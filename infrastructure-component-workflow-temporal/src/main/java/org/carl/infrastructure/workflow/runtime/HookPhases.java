package org.carl.infrastructure.workflow.runtime;

/**
 * Phase constants used by {@link AsyncHookInvocation} and the {@link GenericWorkflowImpl}
 * {@code fireHook} helper to identify which lifecycle point a hook invocation belongs to.
 */
public final class HookPhases {

    public static final String WORKFLOW_START = "WORKFLOW_START";
    public static final String WORKFLOW_END   = "WORKFLOW_END";
    public static final String NODE_ENTER     = "NODE_ENTER";
    public static final String NODE_EXIT      = "NODE_EXIT";
    public static final String NODE_ERROR     = "NODE_ERROR";
    public static final String EVENT          = "EVENT";
    public static final String COMPENSATE     = "COMPENSATE";

    private HookPhases() {
        throw new AssertionError("no instances");
    }
}
