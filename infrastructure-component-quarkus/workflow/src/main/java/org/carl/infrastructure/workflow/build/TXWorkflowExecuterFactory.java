package org.carl.infrastructure.workflow.build;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

import org.carl.infrastructure.statemachine.StateMachine;
import org.carl.infrastructure.workflow.config.WorkerManger;
import org.carl.infrastructure.workflow.core.ITransactionalWorkflow;

public class TXWorkflowExecuterFactory {
    static WorkflowClient workflowClient = WorkerManger.getWorkflowClient();

    static <S, E, C> ITransactionalWorkflow build(
            StateMachine<S, E, C> stateMachine, String workflowStamp) {
        //        WorkerManger.registerWorker(stateMachine.getMachineId());
        return workflowClient.newWorkflowStub(
                ITransactionalWorkflow.class,
                buildOptions(workflowStamp, stateMachine.getMachineId()));
    }

    private static WorkflowOptions buildOptions(String workflowStamp, String taskQueue) {
        return WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowStamp)
                .build();
    }
}
