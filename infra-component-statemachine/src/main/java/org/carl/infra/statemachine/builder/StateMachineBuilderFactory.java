package org.carl.infra.statemachine.builder;

/**
 * StateMachineBuilderFactory
 *
 */
public class StateMachineBuilderFactory {
    public static <S, E, C> StateMachineBuilder<S, E, C> create() {
        return new StateMachineBuilderImpl<>();
    }
}
