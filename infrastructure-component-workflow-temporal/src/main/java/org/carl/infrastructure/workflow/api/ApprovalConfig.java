package org.carl.infrastructure.workflow.api;

import java.util.Map;
import java.util.function.Function;

/**
 * Configuration of an expression-based approval state. Built via {@link ProcessFlow#approval}.
 * Consumed by the engine's {@code advanceViaApproval} method.
 *
 * @param <S> state type
 * @param <E> event type (preserved for WorkflowActivity parameterization)
 * @param <C> context type
 */
public final class ApprovalConfig<S, E, C> {

    private final S state;
    private final Expr expr;
    private final Function<C, Map<String, String>> assignees; // optional, may be null
    private final S approvedTo;
    private final WorkflowActivity<S, E, C> approvedActivity; // may be null
    private final S rejectedTo;
    private final WorkflowActivity<S, E, C> rejectedActivity; // may be null

    ApprovalConfig(
            S state,
            Expr expr,
            Function<C, Map<String, String>> assignees,
            S approvedTo,
            WorkflowActivity<S, E, C> approvedActivity,
            S rejectedTo,
            WorkflowActivity<S, E, C> rejectedActivity) {
        this.state = state;
        this.expr = expr;
        this.assignees = assignees;
        this.approvedTo = approvedTo;
        this.approvedActivity = approvedActivity;
        this.rejectedTo = rejectedTo;
        this.rejectedActivity = rejectedActivity;
    }

    public S state() {
        return state;
    }

    public Expr expr() {
        return expr;
    }

    /**
     * Optional function returning the assignee map (step name → approver id) from context. May be
     * null — the engine does not require it; it is informational for hooks.
     */
    public Function<C, Map<String, String>> assignees() {
        return assignees;
    }

    public S approvedTo() {
        return approvedTo;
    }

    /** May be null (no activity on the approved branch). */
    public WorkflowActivity<S, E, C> approvedActivity() {
        return approvedActivity;
    }

    public S rejectedTo() {
        return rejectedTo;
    }

    /** May be null (no activity on the rejected branch). */
    public WorkflowActivity<S, E, C> rejectedActivity() {
        return rejectedActivity;
    }
}
