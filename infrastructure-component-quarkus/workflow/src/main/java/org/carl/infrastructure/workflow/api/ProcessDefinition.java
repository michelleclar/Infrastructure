package org.carl.infrastructure.workflow.api;

import java.time.Duration;

/**
 * A business process definition. Business implements this with a state-machine graph plus optional
 * per-state {@link WorkflowActivity}s — and never touches {@code io.temporal.*}. The generic engine
 * interprets it.
 *
 * <p>The single entry point is {@link #define(ProcessFlow)}: wire your transitions (orchestration)
 * and bind activities (side effects) using the fluent {@link ProcessFlow} DSL. The engine then
 * dispatches each activity as a Temporal activity, handling timeout, retry, and Saga compensation
 * automatically.
 *
 * @param <S> state type (must be an {@code enum})
 * @param <E> event type
 * @param <C> context type (must be serializable by the Temporal DataConverter)
 */
public interface ProcessDefinition<S, E, C> {

    /** Stable id: used as the Temporal workflow type name and the state-machine id. */
    String id();

    /**
     * Wire the process graph and bind activities.
     *
     * <p>Use the {@link ProcessFlow} DSL to declare transitions and optionally bind a
     * {@link WorkflowActivity} to each target state:
     *
     * <pre>
     * flow.from(SUBMITTED).to(APPROVED).on(APPROVE).when(c -> c.getApprover() != null).run(new DeductBalanceActivity());
     * flow.from(APPROVED).to(DONE).on(CONFIRM).run(new WriteRecordActivity());
     * flow.from(SUBMITTED).to(REJECTED).on(TIMEOUT); // no activity
     * </pre>
     *
     * Called once at registration time; the resulting state machine and activity map are stored in
     * {@link ProcessRegistry}.
     */
    void define(ProcessFlow<S, E, C> flow);

    S startState();

    boolean isTerminal(S state);

    /**
     * How long to wait for the next event at {@code state}; {@code null} means wait indefinitely.
     * Pure function of ctx (evaluated in workflow code).
     */
    default Duration awaitTimeout(S state, C ctx) {
        return null;
    }

    /** Event to fire if {@link #awaitTimeout} elapses at {@code state}; {@code null} = keep waiting. */
    default E onTimeout(S state) {
        return null;
    }

    Class<S> stateType();

    Class<E> eventType();

    Class<C> ctxType();
}
