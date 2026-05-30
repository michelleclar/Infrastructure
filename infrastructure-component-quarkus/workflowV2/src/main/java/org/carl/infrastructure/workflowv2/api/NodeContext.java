package org.carl.infrastructure.workflowv2.api;

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

    public NodeContext(S fromState, S toState, E event, C ctx) {
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.ctx = ctx;
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
}
