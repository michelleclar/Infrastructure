package org.carl.infrastructure.statemachine.builder;

/**
 * To
 *
 */
public interface To<S, E, C> {
    /**
     * Build transition event
     *
     * @param event transition event
     * @return On clause builder
     */
    On<S, E, C> on(E event);
}
