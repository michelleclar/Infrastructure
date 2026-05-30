package org.carl.infrastructure.workflow.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Owns the minimal transition model for a process and provides the fluent inline DSL for wiring
 * transitions. Replaces the old statemachine-library decorator with a self-contained model.
 *
 * <h3>DSL forms</h3>
 *
 * <pre>
 * // Bind a reusable WorkflowActivity to a transition; it owns its name/timeout/retry/compensate.
 * flow.from(SUBMITTED).to(APPROVED).on(APPROVE)
 *     .when(c -> c.getApprover() != null)   // optional guard (pure, workflow code)
 *     .perform(new DeductBalanceActivity());
 *
 * // Routing-only transition: no activity, registered immediately (no closer needed).
 * flow.from(SUBMITTED).to(REJECTED).on(REJECT);
 * </pre>
 *
 * <h3>Transition model</h3>
 *
 * <p>Each call to {@code from(S).to(S).on(E)} records a {@link TransitionSpec} immediately; there
 * is no deferred builder requiring a terminal {@code perform()} call. Optional fluent steps
 * ({@code .when}, {@code .options}, {@code .perform}, {@code .saga}) enrich the spec after
 * recording.
 *
 * <h3>Resolution</h3>
 *
 * <p>Given (from, event, ctx), {@link #resolve} returns the FIRST matching transition — ordered by
 * registration order — whose {@code from} and {@code event} match AND whose guard (if any) passes.
 * Guards are {@link Predicate}{@code <C>}: they run in workflow code and MUST be pure (no IO, no
 * clock, no randomness — Temporal replay will re-execute them deterministically).
 *
 * <p>If no transition matches, returns {@code null} (caller interprets as "stay / keep waiting").
 *
 * <!-- TODO(会签/parallel): resolution is single-target today.  To support parallel/会签 fan-out,
 *      let the model carry multiple targets (or a fork marker), have resolve return a
 *      List&lt;ResolvedTransition&gt;, and update the engine to schedule all branches. -->
 *
 * @param <S> state type
 * @param <E> event type
 * @param <C> context type
 */
public final class ProcessFlow<S, E, C> {

    /** The ordered list of registered transitions. */
    private final List<TransitionSpec<S, E, C>> transitions = new ArrayList<>();

    /**
     * A resolved transition: the target state and the (possibly null) activity to run on entering
     * it.
     */
    public static final class ResolvedTransition<S, E, C> {
        private final S to;
        private final WorkflowActivity<S, E, C> activity; // null → routing-only

        ResolvedTransition(S to, WorkflowActivity<S, E, C> activity) {
            this.to = to;
            this.activity = activity;
        }

        public S to() {
            return to;
        }

        /** May be {@code null} for routing-only transitions. */
        public WorkflowActivity<S, E, C> activity() {
            return activity;
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** A fully-built transition spec (mutable during construction, frozen after {@code from()...on()}). */
    static final class TransitionSpec<S, E, C> {
        final S from;
        final E event;
        final S to;
        Predicate<C> guard; // null → always matches
        WorkflowActivity<S, E, C> activity; // null → routing-only

        TransitionSpec(S from, E event, S to) {
            this.from = from;
            this.event = event;
            this.to = to;
        }
    }

    ProcessFlow() {}

    // ── Transition builder chain ──────────────────────────────────────────────

    /**
     * Start a transition: {@code from(S).to(S).on(E)} [.when(guard)] [.perform(...)].
     *
     * <p>The resulting transition is recorded IMMEDIATELY when {@code .on(E)} is called; no further
     * call is required. Optional steps ({@code .when}, {@code .options}, {@code .perform},
     * {@code .saga}) enrich the already-recorded spec.
     */
    public FlowFrom from(S state) {
        return new FlowFrom(state);
    }

    /**
     * Returns an unmodifiable view of the recorded transition specs. Package-private; consumed by
     * {@link ProcessRegistry}.
     */
    List<TransitionSpec<S, E, C>> transitions() {
        return Collections.unmodifiableList(transitions);
    }

    /**
     * Resolve (from, event, ctx) to the first matching transition.
     *
     * <p>Guards are evaluated here in workflow code — they MUST be pure functions.
     *
     * @return the resolved transition, or {@code null} if none matched.
     */
    @SuppressWarnings("unchecked")
    public ResolvedTransition<S, E, C> resolve(Object from, Object event, C ctx) {
        for (TransitionSpec<S, E, C> spec : transitions) {
            if (!spec.from.equals(from) || !spec.event.equals(event)) {
                continue;
            }
            // guard runs in workflow code — must be pure
            if (spec.guard != null && !spec.guard.test(ctx)) {
                continue;
            }
            return new ResolvedTransition<>(spec.to, spec.activity);
        }
        return null; // no matching transition
    }

    // ── Fluent chain classes ──────────────────────────────────────────────────

    /** Result of {@code flow.from(S)}. */
    public final class FlowFrom {
        private final S fromState;

        FlowFrom(S fromState) {
            this.fromState = fromState;
        }

        public FlowTo to(S state) {
            return new FlowTo(fromState, state);
        }
    }

    /** Result of {@code .to(S)}. */
    public final class FlowTo {
        private final S fromState;
        private final S toState;

        FlowTo(S fromState, S toState) {
            this.fromState = fromState;
            this.toState = toState;
        }

        /**
         * Records the transition immediately and returns a {@link FlowOn} for optional enrichment.
         * A bare call (no further chain) is valid: routing-only transition with no guard/activity.
         */
        public FlowOn on(E event) {
            TransitionSpec<S, E, C> spec = new TransitionSpec<>(fromState, event, toState);
            transitions.add(spec);
            return new FlowOn(spec);
        }
    }

    /**
     * Builder returned after {@code .on(E)} — the transition is already recorded; steps here are
     * optional enrichment.
     */
    public final class FlowOn {
        private final TransitionSpec<S, E, C> spec;

        FlowOn(TransitionSpec<S, E, C> spec) {
            this.spec = spec;
        }

        /**
         * Add a guard condition (pure, workflow code only — no IO).
         *
         * @param guard a {@link Predicate}{@code <C>}; evaluated in workflow code, must be
         *     deterministic and side-effect-free.
         */
        public FlowGuard when(Predicate<C> guard) {
            spec.guard = guard;
            return new FlowGuard(spec);
        }

        /**
         * Bind a {@link WorkflowActivity} class (reusable form). The activity owns its name,
         * timeout, retry, and compensation.
         */
        public void perform(WorkflowActivity<S, E, C> activity) {
            spec.activity = activity;
        }
    }

    /**
     * Builder after {@code .when(guard)} — same perform options, now with guard set.
     */
    public final class FlowGuard {
        private final TransitionSpec<S, E, C> spec;

        FlowGuard(TransitionSpec<S, E, C> spec) {
            this.spec = spec;
        }

        /**
         * Bind a {@link WorkflowActivity} class. The transition's guard is already set.
         */
        public void perform(WorkflowActivity<S, E, C> activity) {
            spec.activity = activity;
        }
    }
}
