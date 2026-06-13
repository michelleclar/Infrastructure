package org.carl.infrastructure.workflow.handlers;

/**
 * Canonical {@link org.carl.infrastructure.workflow.definition.NodeResult#payload()} keys used by
 * built-in handlers to communicate intent to the workflow runtime (Wave 3).
 *
 * <p>Handlers in this package never directly execute side effects (e.g. call activities, schedule
 * timers, start sub-processes). Instead, when a handler's {@code run()} returns {@link
 * org.carl.infrastructure.workflow.definition.NodeResult#waiting()}, the runtime inspects the
 * result's {@code payload} keyed by these constants and performs the requested action. This keeps
 * the core module zero-Temporal-dependency.
 */
public final class RuntimeIntents {

    // serviceTask: tell runtime which activity to invoke.
    /** Activity name (string). */
    public static final String ACTIVITY = "activity";

    /** Activity input (Map&lt;String,Object&gt;). */
    public static final String ACTIVITY_INPUT = "activityInput";

    // approvalTask / userTask / eventTask.
    /** Assignee identifier or expression (string). */
    public static final String ASSIGNEE = "assignee";

    /**
     * Event name that the handler waits for (string). Used by approvalTask, userTask, and
     * eventTask alike.
     */
    public static final String AWAIT_EVENT = "awaitEvent";

    /** ISO-8601 timeout duration (string, e.g. {@code "PT24H"}). */
    public static final String TIMEOUT_DURATION = "timeoutDuration";

    // timerTask.
    /** ISO-8601 duration (string). */
    public static final String DURATION = "duration";

    // taskGroup.
    /** Child node definitions (List&lt;Map&lt;String,Object&gt;&gt;). */
    public static final String CHILDREN = "children";

    /** Join rule: {@code "all"} or {@code "any"} (string). */
    public static final String JOIN_RULE = "joinRule";

    // subProcess.
    /** Sub-workflow id (string). */
    public static final String SUB_WORKFLOW_ID = "subWorkflowId";

    /**
     * Serialized {@link org.carl.infrastructure.workflow.definition.WorkflowDefinition} JSON for
     * the child workflow (string).
     */
    public static final String SUB_DEFINITION_JSON = "subDefinitionJson";

    /** Sub-workflow input (Map&lt;String,Object&gt;). */
    public static final String SUB_INPUT = "subInput";

    // ---- Variable mutation ----

    /**
     * Variables to merge into the workflow's mutable variable map (Map&lt;String,Object&gt;). A
     * handler may include this key in the {@link
     * org.carl.infrastructure.workflow.definition.NodeResult#payload()} of <em>any</em> result
     * (WAITING or terminal). The runtime applies each entry to the variable map outside the
     * deterministic boundary, before routing the node's outgoing edge, so guard expressions
     * ({@code ${var}}) observe the new values. Last write wins on key collision.
     */
    public static final String SET_VARIABLES = "setVariables";

    // ---- Hook dispatch (Wave 3 interceptor) ----

    /**
     * Hook phase: which lifecycle point fired. Values: WORKFLOW_START, WORKFLOW_END, NODE_ENTER,
     * NODE_EXIT, NODE_ERROR, EVENT, COMPENSATE (string).
     */
    public static final String HOOK_PHASE = "hookPhase";

    /** Hook payload (Map&lt;String,Object&gt;). Shape depends on phase. */
    public static final String HOOK_PAYLOAD = "hookPayload";

    /** Optional ordered list of async interceptor class names to invoke (List&lt;String&gt;). */
    public static final String HOOK_INTERCEPTORS = "hookInterceptors";

    private RuntimeIntents() {
        throw new AssertionError("no instances");
    }
}
