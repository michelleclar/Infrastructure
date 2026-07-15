package org.carl.infra.statemachine.builder;

/**
 * InternalTransitionBuilder
 *
 */
public interface InternalTransitionBuilder<S, E, C> {
    /**
     * Build a internal transition
     *
     * @param stateId id of transition
     * @return To clause builder
     */
    To<S, E, C> within(S stateId);
}
