package org.carl.infrastructure.statemachine;

/**
 * IStateMachineAbility
 *
 * <p>Mixin interface that exposes a {@link StateMachine} instance to the implementing class.
 * Follow the Ability pattern: implement this interface on a context or service class rather than
 * injecting the state machine directly.
 *
 * @param <S> the type of state
 * @param <E> the type of event
 * @param <C> the user-defined context
 */
public interface IStateMachineAbility<S, E, C> {

    /**
     * Returns the {@link StateMachine} associated with this ability.
     *
     * @return the state machine instance; never {@code null}
     */
    StateMachine<S, E, C> getStateMachine();
}
