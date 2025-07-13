package org.carl.infrastructure.workflow.config;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import java.util.HashMap;
import java.util.Map;

public class WorkerManger {
    protected static Map<String, Worker> workerMap = new HashMap<>();
    protected static WorkflowClient workflowClient;

    public static WorkflowClient getWorkflowClient() {
        return workflowClient;
    }

    public static Worker getWorker(String workerName) {
        return workerMap.get(workerName);
    }

    /**
     * maybe need check taskQueue duplicate
     *
     * @param taskQueue machineId
     */
    @BATE
    public static void registerWorker(String taskQueue) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(taskQueue);
        workerMap.put(taskQueue, worker);
        factory.start();
    }

    public static void registerActivity(String workerName, Object activityImplementation) {
        Worker worker = getWorker(workerName);
        worker.registerActivitiesImplementations(activityImplementation);
    }
}
