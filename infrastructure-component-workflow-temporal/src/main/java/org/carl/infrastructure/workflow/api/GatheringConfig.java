package org.carl.infrastructure.workflow.api;

import java.util.Collection;
import java.util.function.Function;

/**
 * Configuration of a multi-approver gathering state: who votes, the quorum policy, and where to
 * advance on each outcome.
 *
 * <p>Built via {@link ProcessFlow#gather(Object)}. Consumed by the engine.
 *
 * @param <S> state type
 * @param <E> event type (unused here, but kept for the {@link WorkflowActivity} parameterization)
 * @param <C> context type
 */
public final class GatheringConfig<S, E, C> {

    private final S state;
    private final Function<C, Collection<String>> assignees;
    private final ApprovalPolicy policy;
    private final S approvedTo;
    private final WorkflowActivity<S, E, C> approvedActivity; // may be null
    private final S rejectedTo;
    private final WorkflowActivity<S, E, C> rejectedActivity; // may be null

    GatheringConfig(
            S state,
            Function<C, Collection<String>> assignees,
            ApprovalPolicy policy,
            S approvedTo,
            WorkflowActivity<S, E, C> approvedActivity,
            S rejectedTo,
            WorkflowActivity<S, E, C> rejectedActivity) {
        this.state = state;
        this.assignees = assignees;
        this.policy = policy;
        this.approvedTo = approvedTo;
        this.approvedActivity = approvedActivity;
        this.rejectedTo = rejectedTo;
        this.rejectedActivity = rejectedActivity;
    }

    public S state() {
        return state;
    }

    public Function<C, Collection<String>> assignees() {
        return assignees;
    }

    public ApprovalPolicy policy() {
        return policy;
    }

    public S approvedTo() {
        return approvedTo;
    }

    public WorkflowActivity<S, E, C> approvedActivity() {
        return approvedActivity;
    }

    public S rejectedTo() {
        return rejectedTo;
    }

    public WorkflowActivity<S, E, C> rejectedActivity() {
        return rejectedActivity;
    }
}
