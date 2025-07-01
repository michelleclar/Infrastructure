package org.carl.infrastructure.component.web.config;

import io.quarkus.runtime.StartupEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.carl.infrastructure.component.web.ability.IWorkerAbility;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class WorkerLifecycle {

    private static final Logger LOGGER = Logger.getLogger(WorkerLifecycle.class);
    @Inject Instance<IWorkerAbility> workerAbilityList;
    @Inject WorkflowClient client;

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("beg init worker");
        // check is duplicate
        Set<String> workers = new HashSet<>();
        WorkerFactory factory = WorkerFactory.newInstance(client);
        for (IWorkerAbility workerAbility : workerAbilityList) {
            if (workerAbility.getWorker().isEmpty()) {
                throw new RuntimeException("worker name doesn't is empty or null");
            }
            if (!WorkerManger.workerMap.containsKey(workerAbility.getWorker())) {
                Worker worker = factory.newWorker(workerAbility.getWorker());
                WorkerManger.workerMap.put(workerAbility.getWorker(), worker);
            }
            Worker worker = WorkerManger.workerMap.get(workerAbility.getWorker());
            worker.registerActivitiesImplementations(workerAbility.getActivityImplInstances());
            worker.registerWorkflowImplementationTypes(workerAbility.getWorkflowImplClasses());
            LOGGER.infof("Worker [%s] is init", workerAbility.getWorker());
        }
        factory.start();
        LOGGER.infof("worker [%s] is started", WorkerManger.workerMap.keySet());
    }
}
