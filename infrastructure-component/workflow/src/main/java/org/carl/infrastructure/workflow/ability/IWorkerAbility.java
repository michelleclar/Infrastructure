package org.carl.infrastructure.workflow.ability;

import io.temporal.client.WorkflowOptions;

import org.carl.infrastructure.workflow.config.WorkerManger;

import java.util.UUID;

public interface IWorkerAbility {

    /**
     * Worker Identification
     *
     * @return worker name
     */
    String getWorker();

    /**
     * workflow class
     *
     * @return class
     */
    Class<?>[] getWorkflowImplClasses();

    Object[] getActivityImplInstances();

    default <T> T createWorkflow(Class<T> workflowImplClass) {
        return WorkerManger.getWorkflowClient()
                .newWorkflowStub(workflowImplClass, options(generateWorkflowId()));
    }

    default String generateWorkflowId() {
        UUID uuid = UUID.randomUUID();
        return getWorker() + "-" + uuid;
    }

    default WorkflowOptions options(String workerId) {

        return WorkflowOptions.newBuilder()
                .setTaskQueue(getWorker())
                .setWorkflowId(workerId)
                .build();
    }
}
