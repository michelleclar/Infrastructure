package org.carl.infrastructure.workflow.build;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.workflow.config.WorkerManger;

public class TXWorkflowBuilder<S, E, C> {
    private final StateMachine<S, E, C> stateMachine;

    TXWorkflowBuilder(StateMachine<S, E, C> stateMachine) {
        this.stateMachine = stateMachine;
    }

    public static <S, E, C> TXWorkflowBuilder<S, E, C> of(StateMachine<S, E, C> stateMachine) {
        WorkerManger.registerWorker(stateMachine.getMachineId());
        return new TXWorkflowBuilder<>(stateMachine);
    }

    public TXWorkflowExecuter<S, E, C> txStamp(String txId) {
        return new TXWorkflowExecuter<>(txId, stateMachine);
    }
}
