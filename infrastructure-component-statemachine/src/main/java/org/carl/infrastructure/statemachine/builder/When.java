package org.carl.infrastructure.statemachine.builder;

import org.carl.infrastructure.statemachine.Action;

/**
 * When
 *
 */
public interface When<S, E, C> {
    /**
     * Define action to be performed during transition
     *
     * @param action performed action
     */
    void perform(Action<S, E, C> action);
}
