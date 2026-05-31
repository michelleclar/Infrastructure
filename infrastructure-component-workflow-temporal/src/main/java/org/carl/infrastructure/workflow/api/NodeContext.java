package org.carl.infrastructure.workflow.api;

/**
 * <ul>
 *   <li>For the <b>start-state</b> entry node, {@code fromState} and {@code event} are {@code null}.
 *   <li>For a <b>compensation</b> node, these carry the transition captured <i>at the time the
 *       forward step ran</i> — so a compensation knows exactly which state/transition it is rolling
 *       back (not the state at failure time).
 * </ul>
 *
 * @param <S> state type
 * @param <E> event type
 * @param <C> context type
 */
public final class NodeContext<S, E, C> {

    private final S fromState;
    private final S toState;
    private final E event;
    private final C ctx;
    private final String step; // null for non-hook activities

    /** Legacy constructor — step defaults to null. Preserves backward compatibility. */
    public NodeContext(S fromState, S toState, E event, C ctx) {
        this(fromState, toState, event, ctx, null);
    }

    /** Full constructor including the hook step name (null for non-hook activities). */
    public NodeContext(S fromState, S toState, E event, C ctx, String step) {
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.ctx = ctx;
        this.step = step;
    }

    /** State the process was in before this transition; {@code null} for the start-state node. */
    public S fromState() {
        return fromState;
    }

    /** State that was entered (whose node this is / whose entry this compensates). */
    public S toState() {
        return toState;
    }

    /** Event that triggered the transition; {@code null} for the start-state node. */
    public E event() {
        return event;
    }

    /** Business context. */
    public C ctx() {
        return ctx;
    }

    /**
     * The step name when this context is delivered to a hook activity ({@code before}, {@code after},
     * or {@code onComplete}); {@code null} for outcome and transition activities.
     */
    public String step() {
        return step;
    }
}
