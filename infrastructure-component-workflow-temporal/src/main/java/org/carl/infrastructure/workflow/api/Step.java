package org.carl.infrastructure.workflow.api;

/**
 * A leaf node in an approval {@link Expr} tree — represents a single named vote.
 *
 * <p>Holds the step name and three optional hook activities set at definition time:
 *
 * <ul>
 *   <li>{@link #beforeHook} — fires once at state entry, before any votes.
 *   <li>{@link #afterHook} — fires after every vote received for this step.
 *   <li>{@link #onCompleteHook} — fires when this step transitions from UNKNOWN to a terminal value
 *       (TRUE or FALSE); fires again if cleared by SENDBACK and then re-decided.
 * </ul>
 *
 * <p>Fluent setters return {@code this} for chaining:
 *
 * <pre>
 *   step("manager")
 *       .before(new NotifyApprover())
 *       .after(new AuditVote())
 *       .onComplete(new ArchiveStep())
 * </pre>
 */
public final class Step implements Expr {

    final String name;
    WorkflowActivity<?, ?, ?> beforeHook;
    WorkflowActivity<?, ?, ?> afterHook;
    WorkflowActivity<?, ?, ?> onCompleteHook;

    /** Returns the before hook activity, or null. */
    public WorkflowActivity<?, ?, ?> beforeHook() { return beforeHook; }

    /** Returns the after hook activity, or null. */
    public WorkflowActivity<?, ?, ?> afterHook() { return afterHook; }

    /** Returns the onComplete hook activity, or null. */
    public WorkflowActivity<?, ?, ?> onCompleteHook() { return onCompleteHook; }

    Step(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Step name must be non-blank");
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    /** Register a hook that fires once when the approval state is entered, before any votes. */
    public Step before(WorkflowActivity<?, ?, ?> activity) {
        this.beforeHook = activity;
        return this;
    }

    /** Register a hook that fires after each vote (APPROVE / REJECT / SENDBACK) for this step. */
    public Step after(WorkflowActivity<?, ?, ?> activity) {
        this.afterHook = activity;
        return this;
    }

    /**
     * Register a hook that fires when this step transitions from UNKNOWN to a terminal value. Fires
     * again if cleared by SENDBACK and subsequently re-decided.
     */
    public Step onComplete(WorkflowActivity<?, ?, ?> activity) {
        this.onCompleteHook = activity;
        return this;
    }
}
