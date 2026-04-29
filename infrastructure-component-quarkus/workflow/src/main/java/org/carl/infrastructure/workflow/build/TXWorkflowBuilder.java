package org.carl.infrastructure.workflow.build;

import org.carl.infrastructure.statemachine.StateMachine;

public class TXWorkflowBuilder<S, E, C> {
    private final StateMachine<S, E, C> stateMachine;

    TXWorkflowBuilder(StateMachine<S, E, C> stateMachine) {
        this.stateMachine = stateMachine;
    }

    public static <S, E, C> TXWorkflowBuilder<S, E, C> of(StateMachine<S, E, C> stateMachine) {
        return new TXWorkflowBuilder<>(stateMachine);
    }

    public TXWorkflowExecuter<S, E, C> entityId(String entityId) {
        return new TXWorkflowExecuter<>(entityId, stateMachine);
    }
}
