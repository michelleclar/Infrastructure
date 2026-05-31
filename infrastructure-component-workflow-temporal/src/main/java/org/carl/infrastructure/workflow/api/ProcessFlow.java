package org.carl.infrastructure.workflow.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
 *
 * // Expression-based approval state (会签 v2):
 * flow.approval(SUBMITTED)
 *     .expr(and(step("manager"), or(step("legal"), step("compliance"))))
 *     .onApproved(APPROVED)
 *     .onRejected(REJECTED);
 * </pre>
 *
 * <h3>Transition model</h3>
 *
 * <p>Each call to {@code from(S).to(S).on(E)} records a {@link TransitionSpec} immediately; there
 * is no deferred builder requiring a terminal {@code perform()} call. Optional fluent steps
 * ({@code .when}, {@code .perform}) enrich the spec after recording.
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
 * <h3>Approval (会签)</h3>
 *
 * <p>A state may instead be declared an <em>approval state</em>: it collects {@code vote} signals
 * and evaluates a boolean expression tree ({@link Expr}) over named vote leaves, advancing to one
 * of two configured outcome states (approved / rejected) when the expression reaches a terminal
 * value. Use {@link #approval(Object)} to declare one. An approval state may NOT also have plain
 * {@code from(state).to(...).on(event)} transitions (validated by {@link ProcessRegistry}).
 *
 * @param <S> state type
 * @param <E> event type
 * @param <C> context type
 */
public final class ProcessFlow<S, E, C> {

    /** The ordered list of registered transitions. */
    private final List<TransitionSpec<S, E, C>> transitions = new ArrayList<>();

    /** Approval states: state → its approval config. */
    private final Map<S, ApprovalConfig<S, E, C>> approvals = new HashMap<>();

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
     * call is required. Optional steps ({@code .when}, {@code .perform}) enrich the already-recorded
     * spec.
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
     * Returns an unmodifiable view of the registered approval states (state → config). Package-
     * private; consumed by {@link ProcessRegistry}.
     */
    Map<S, ApprovalConfig<S, E, C>> approvals() {
        return Collections.unmodifiableMap(approvals);
    }

    /**
     * Declare {@code state} as an expression-based approval state. Returns a builder chain:
     *
     * <pre>
     *   flow.approval(SUBMITTED)
     *       .expr(and(step("manager"), or(step("legal"), step("compliance"))))
     *       .assignees(c -> Map.of("manager", c.getManagerId(), ...))  // optional
     *       .onApproved(APPROVED).perform(new RecordApproval())         // .perform optional
     *       .onRejected(REJECTED).perform(new RecordRejection());       // .perform optional
     * </pre>
     *
     * @throws IllegalStateException if the same state is declared as approval twice
     */
    public ApprovalExpr approval(S state) {
        if (approvals.containsKey(state)) {
            throw new IllegalStateException(
                    "Approval already declared for state [" + state + "]");
        }
        return new ApprovalExpr(state);
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

    // ── Approval DSL chain ────────────────────────────────────────────────────

    /** Step 1: approval(state).<b>expr(...)</b>. */
    public final class ApprovalExpr {
        private final S state;

        ApprovalExpr(S state) {
            this.state = state;
        }

        public ApprovalAssignees expr(Expr expression) {
            return new ApprovalAssignees(state, expression);
        }
    }

    /** Step 2 (optional): ....<b>assignees(...)</b> or directly <b>onApproved(...)</b>. */
    public final class ApprovalAssignees {
        private final S state;
        private final Expr expr;

        ApprovalAssignees(S state, Expr expr) {
            this.state = state;
            this.expr = expr;
        }

        /** Optional assignee map (step name → approver id); informational for hooks. */
        public ApprovalOnApproved assignees(Function<C, Map<String, String>> assigneeFn) {
            return new ApprovalOnApproved(state, expr, assigneeFn);
        }

        /** Skip assignees and go directly to onApproved. */
        public ApprovalApprovedActivity onApproved(S approvedTo) {
            return new ApprovalOnApproved(state, expr, null).onApproved(approvedTo);
        }
    }

    /** Step 3: ....<b>onApproved(state)</b>[.perform(activity)]. */
    public final class ApprovalOnApproved {
        private final S state;
        private final Expr expr;
        private final Function<C, Map<String, String>> assignees; // may be null

        ApprovalOnApproved(S state, Expr expr, Function<C, Map<String, String>> assignees) {
            this.state = state;
            this.expr = expr;
            this.assignees = assignees;
        }

        public ApprovalApprovedActivity onApproved(S approvedTo) {
            return new ApprovalApprovedActivity(state, expr, assignees, approvedTo);
        }
    }

    /** Step 4a: ....onApproved(state).<b>perform(activity)?</b> then onRejected(...). */
    public final class ApprovalApprovedActivity {
        private final S state;
        private final Expr expr;
        private final Function<C, Map<String, String>> assignees;
        private final S approvedTo;
        private WorkflowActivity<S, E, C> approvedActivity; // optional

        ApprovalApprovedActivity(
                S state,
                Expr expr,
                Function<C, Map<String, String>> assignees,
                S approvedTo) {
            this.state = state;
            this.expr = expr;
            this.assignees = assignees;
            this.approvedTo = approvedTo;
        }

        /** Optional activity to run on entering the approved outcome state. */
        public ApprovalApprovedActivity perform(WorkflowActivity<S, E, C> activity) {
            this.approvedActivity = activity;
            return this;
        }

        /** REQUIRED: set the rejected outcome target. */
        public ApprovalRejectedActivity onRejected(S rejectedTo) {
            return new ApprovalRejectedActivity(
                    state, expr, assignees, approvedTo, approvedActivity, rejectedTo);
        }
    }

    /** Step 4b: ....onRejected(state).<b>perform(activity)?</b> — closes the approval declaration. */
    public final class ApprovalRejectedActivity {
        private final S state;
        private final Expr expr;
        private final Function<C, Map<String, String>> assignees;
        private final S approvedTo;
        private final WorkflowActivity<S, E, C> approvedActivity;
        private final S rejectedTo;

        ApprovalRejectedActivity(
                S state,
                Expr expr,
                Function<C, Map<String, String>> assignees,
                S approvedTo,
                WorkflowActivity<S, E, C> approvedActivity,
                S rejectedTo) {
            this.state = state;
            this.expr = expr;
            this.assignees = assignees;
            this.approvedTo = approvedTo;
            this.approvedActivity = approvedActivity;
            this.rejectedTo = rejectedTo;
            // Register with no rejected activity by default; perform(...) replaces.
            approvals.put(
                    state,
                    new ApprovalConfig<>(
                            state,
                            expr,
                            assignees,
                            approvedTo,
                            approvedActivity,
                            rejectedTo,
                            null));
        }

        /** Optional activity to run on entering the rejected outcome state. */
        public void perform(WorkflowActivity<S, E, C> activity) {
            approvals.put(
                    state,
                    new ApprovalConfig<>(
                            state,
                            expr,
                            assignees,
                            approvedTo,
                            approvedActivity,
                            rejectedTo,
                            activity));
        }
    }
}
