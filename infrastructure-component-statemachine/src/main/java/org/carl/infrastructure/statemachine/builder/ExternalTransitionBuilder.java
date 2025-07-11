package org.carl.infrastructure.statemachine.builder;

/**
 * ExternalTransitionBuilder
 *
 */
public interface ExternalTransitionBuilder<S, E, C> {
    /**
     * Build transition source state.
     *
     * @param stateId id of state
     * @return from clause builder
     */
    From<S, E, C> from(S stateId);
}
