package org.carl.infrastructure.statemachine;

@FunctionalInterface
public interface ActionWrapper<S, E, C> {
    Action<S, E, C> wrap(Action<S, E, C> next);
}
