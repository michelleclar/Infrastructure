package org.carl.infrastructure.statemachine.builder;

/**
 * Default fail callback, do nothing.
 *
 */
public class NumbFailCallback<S, E, C> implements FailCallback<S, E, C> {

    @Override
    public void onFail(S sourceState, E event, C context) {
        // do nothing
    }
}
