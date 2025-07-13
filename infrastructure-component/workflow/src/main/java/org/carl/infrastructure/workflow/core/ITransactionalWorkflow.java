package org.carl.infrastructure.workflow.core;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ITransactionalWorkflow {
    @WorkflowMethod
    <S, E, C> S fireEvent(String machineId, S from, E event, C ctx);
}
