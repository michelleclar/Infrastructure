package org.carl.infrastructure.workflowv2.runtime;

import io.quarkus.runtime.StartupEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflowv2.api.ProcessDefinition;
import org.carl.infrastructure.workflowv2.api.ProcessRegistry;
import org.carl.infrastructure.workflowv2.engine.GenericActivity;
import org.carl.infrastructure.workflowv2.engine.GenericWorkflow;
import org.jboss.logging.Logger;

/**
 * Production wiring: registers every {@link ProcessDefinition} CDI bean, then starts a Temporal
 * worker (on a dedicated task queue) hosting the single generic workflow + activity.
 *
 * <p>Note: the in-memory test exercises the engine directly via {@code TestWorkflowEnvironment};
 * this bean is the real-app path and reuses the {@link WorkflowClient} bean from quarkus-temporal.
 */
@ApplicationScoped
public class WorkflowV2Bootstrap {

    private static final Logger LOG = Logger.getLogger(WorkflowV2Bootstrap.class);

    @Inject WorkflowClient client;
    @Inject WorkflowV2Config config;
    @Inject Instance<ProcessDefinition<?, ?, ?>> processes;

    private WorkerFactory factory;

    void onStart(@Observes StartupEvent event) {
        int count = 0;
        for (ProcessDefinition<?, ?, ?> def : processes) {
            ProcessRegistry.register(def);
            count++;
        }

        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(config.taskQueue());
        worker.registerWorkflowImplementationTypes(GenericWorkflow.class);
        worker.registerActivitiesImplementations(new GenericActivity());
        factory.start();

        LOG.infof(
                "workflowV2 started: task-queue=%s, processes=%d", config.taskQueue(), count);
    }

    @PreDestroy
    void shutdown() {
        if (factory != null) {
            factory.shutdown();
        }
    }
}
