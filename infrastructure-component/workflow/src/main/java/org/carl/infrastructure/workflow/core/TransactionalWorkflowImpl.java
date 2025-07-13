package org.carl.infrastructure.workflow.core;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.statemachine.StateMachineFactory;

public class TransactionalWorkflowImpl implements ITransactionalWorkflow {

    @Override
    public <S, E, C> S fireEvent(String machineId, S from, E event, C ctx) {
        StateMachine<S, E, C> stateMachine = StateMachineFactory.get(machineId);
        return stateMachine.fireEvent(from, event, ctx);
    }
}
