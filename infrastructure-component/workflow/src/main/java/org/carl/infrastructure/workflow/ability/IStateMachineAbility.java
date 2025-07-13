package org.carl.infrastructure.workflow.ability;

import jakarta.annotation.PostConstruct;

import org.carl.infrastructure.statemachine.builder.StateMachineBuilder;
import org.carl.infrastructure.statemachine.builder.StateMachineBuilderFactory;
import org.carl.infrastructure.workflow.config.WorkerManger;

/**
 * TODO: add wrapper persistence
 *
 * <p>provide query current state and transfer history
 *
 * @param <S> state
 * @param <E> event
 * @param <C> context
 */
public interface IStateMachineAbility<S, E, C> {
    @PostConstruct
    default void init() {
        StateMachineBuilder<S, E, C> builder = StateMachineBuilderFactory.create();
        externalTransition(builder);
        builder.build(machineId());
        // can be register task queue on temporal, TODO: need check
        WorkerManger.registerWorker(machineId());
    }

    String machineId();

    /**
     * add transition ruler
     *
     * @param stateMachine biz
     */
    void externalTransition(StateMachineBuilder<S, E, C> stateMachine);
}
