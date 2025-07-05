package org.carl.infrastructure.workflow.config;

import io.temporal.worker.Worker;

import java.util.HashMap;
import java.util.Map;

public class WorkerManger {
    protected static Map<String, Worker> workerMap = new HashMap<>();

    // recode

    public static Worker getWorker(String workerName) {
        return workerMap.get(workerName);
    }

    public static void registerActivity(String workerName, Object activityImplementation) {
        Worker worker = getWorker(workerName);
        worker.registerActivitiesImplementations(activityImplementation);
    }
}
