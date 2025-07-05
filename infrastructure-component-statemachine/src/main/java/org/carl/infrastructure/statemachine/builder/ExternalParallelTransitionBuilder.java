package org.carl.infrastructure.statemachine.builder;

public interface ExternalParallelTransitionBuilder<S, E, C> {
    /**
     * Build transition source state.
     *
     * @param stateId id of state
     * @return from clause builder
     */
    ParallelFrom<S, E, C> from(S stateId);
}
