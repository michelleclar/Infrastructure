package org.carl.infrastructure.workflow.build;

import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.workflow.Functions;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.workflow.core.ITransactionalWorkflow;

public class TXWorkflowExecuter<S, E, C> {
    private final String txStamp;
    private final StateMachine<S, E, C> stateMachine;

    TXWorkflowExecuter(String txId, StateMachine<S, E, C> stateMachine) {
        this.txStamp = txId;
        this.stateMachine = stateMachine;
    }

    public WorkflowExecution fireEvent(S from, E event, C ctx) {
        ITransactionalWorkflow workflow = TXWorkflowExecuterFactory.build(stateMachine, txStamp);

        return WorkflowClient.start(
                (Functions.Proc4<String, S, E, C>) workflow::fireEvent,
                stateMachine.getMachineId(),
                from,
                event,
                ctx);
    }
}
