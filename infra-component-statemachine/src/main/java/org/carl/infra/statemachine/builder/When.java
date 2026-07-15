package org.carl.infra.statemachine.builder;

import org.carl.infra.statemachine.Action;

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
