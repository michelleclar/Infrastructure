package org.carl.infrastructure.statemachine;

import org.carl.infrastructure.statemachine.impl.TransitionType;

import java.util.Collection;
import java.util.List;

/**
 * State
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public interface State<S, E, C> extends Visitable {

    /**
     * Gets the state identifier.
     *
     * @return the state identifiers
     */
    S getId();

    /**
     * Add transition to the state
     *
     * @param event the event of the Transition
     * @param target the target of the transition
     * @return
     */
    Transition<S, E, C> addTransition(
            E event, State<S, E, C> target, TransitionType transitionType);

    List<Transition<S, E, C>> addTransitions(
            E event, List<State<S, E, C>> targets, TransitionType transitionType);

    List<Transition<S, E, C>> getEventTransitions(E event);

    Collection<Transition<S, E, C>> getAllTransitions();
}
