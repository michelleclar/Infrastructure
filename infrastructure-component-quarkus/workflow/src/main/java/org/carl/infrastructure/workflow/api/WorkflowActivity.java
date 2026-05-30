package org.carl.infrastructure.workflow.api;

import java.time.Duration;

/**
 * A named, registered activity in a process — the side-effecting unit of work attached to a target
 * state. Business implements this instead of the old loose {@code Node} / {@code NodeSpec} pair.
 *
 * <p><b>Determinism contract (critical for Temporal):</b>
 *
 * <ul>
 *   <li>{@link #run} and {@link #compensate} execute inside a Temporal <em>Activity</em> — IO,
 *       network calls, and blocking are all fine here. Both receive the full {@link NodeContext}
 *       (from-state, to-state, triggering event, business context) so they can act based on the
 *       exact state/transition they are responsible for.
 *   <li>{@link #timeout} and {@link #retry} are evaluated inside <em>workflow</em> code to build
 *       activity options before scheduling the activity. They MUST therefore be <b>pure functions of
 *       {@code ctx}</b> — no IO, no clock reads, no randomness. The state is implicit (this activity
 *       is bound to one target state via the flow DSL).
 * </ul>
 *
 * <p><b>Compensation dispatch naming:</b> the forward call is dispatched under {@link #name()};
 * the compensation call (if {@link #compensable()} is {@code true}) is dispatched under
 * {@code name() + "#compensate"}. The engine registers both in {@link ProcessRegistry} so the
 * dynamic activity dispatcher routes correctly.
 *
 * @param <S> state type (must be an {@code enum})
 * @param <E> event type
 * @param <C> context type (must be serializable by the Temporal DataConverter)
 */
public interface WorkflowActivity<S, E, C> {

    /**
     * Identity of this activity; also used as the Temporal activity-type name for the forward call.
     * Must be unique across all activities registered in the same process.
     */
    String name();

    /**
     * The side effect. Runs as a Temporal Activity — IO is allowed.
     *
     * @param in the full transition that led to this state (from/to/event/ctx)
     */
    void run(NodeContext<S, E, C> in);

    /**
     * Timeout for the forward {@link #run} call; pure function of ctx, evaluated in workflow code.
     * Defaults to 30 seconds.
     */
    default Duration timeout(C ctx) {
        return Duration.ofSeconds(30);
    }

    /**
     * Retry policy for the forward {@link #run} call; pure function of ctx, evaluated in workflow
     * code. Defaults to 3 max attempts with exponential backoff.
     */
    default RetryPolicy retry(C ctx) {
        return RetryPolicy.of(3);
    }

    /**
     * Whether this activity supports compensation (rollback). When {@code true}, the engine
     * registers a Saga compensation after each successful {@link #run} that will invoke
     * {@link #compensate} if a later step fails.
     */
    default boolean compensable() {
        return false;
    }

    /**
     * Rollback logic. Runs as a Temporal Activity (IO allowed). The {@link NodeContext} carries the
     * <em>same</em> from/to/event/ctx that was captured when the forward {@link #run} succeeded, so
     * the compensation knows exactly which state/transition it is rolling back — not the state at
     * failure time.
     *
     * <p>Only called when {@link #compensable()} returns {@code true}.
     */
    default void compensate(NodeContext<S, E, C> in) {}
}
