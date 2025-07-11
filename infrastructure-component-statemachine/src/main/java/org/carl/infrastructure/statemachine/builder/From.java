package org.carl.infrastructure.statemachine.builder;

/**
 * From
 *
 */
public interface From<S, E, C> {
    /**
     * Build transition target state and return to clause builder
     *
     * @param stateId id of state
     * @return To clause builder
     */
    To<S, E, C> to(S stateId);
}
