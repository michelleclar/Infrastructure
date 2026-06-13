package org.carl.infrastructure.workflow.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.StartupEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflow.archive.ArchiveActivities;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Production wiring: at startup, assembles the engine's registries from CDI beans and starts a
 * single Temporal worker (on a dedicated task queue) hosting the generic workflow + activity.
 *
 * <p>What it collects from the CDI container:
 *
 * <ul>
 *   <li>built-in node handlers (always) plus every {@link NodeHandler} bean the app provides — these
 *       go through {@code NodeHandlerRegistry.register}, which runs the determinism guard, so custom
 *       handlers should be {@code @Singleton} (an {@code @ApplicationScoped} client proxy would hide
 *       the real class from the guard);
 *   <li>an optional {@link BusinessActivityRegistry} bean (service-task implementations) — empty if
 *       none is produced;
 *   <li>an optional {@link ArchiveActivities} bean (termination archival);
 *   <li>an optional {@link WorkflowInterceptorRegistry} bean (lifecycle hooks);
 *   <li>the application {@link ObjectMapper}, installed into {@link ObjectMapperHolder} so the engine
 *       serialises node config/state with the same Jackson configuration as the rest of the app.
 * </ul>
 *
 * <p>Reuses the {@link WorkflowClient} bean produced by the quarkus-temporal extension.
 */
@ApplicationScoped
public class WorkflowBootstrap {

    private static final Logger log = LoggerFactory.getLogger(WorkflowBootstrap.class);

    @Inject WorkflowClient client;
    @Inject WorkflowConfig config;

    @Inject Instance<NodeHandler<?, ?, ?>> nodeHandlers;
    @Inject Instance<BusinessActivityRegistry> activityRegistry;
    @Inject Instance<ArchiveActivities> archiveActivities;
    @Inject Instance<WorkflowInterceptorRegistry> interceptors;
    @Inject Instance<ObjectMapper> objectMapper;

    private WorkerFactory factory;

    void onStart(@Observes StartupEvent event) {
        // Align the engine's in-workflow JSON handling with the application's Jackson config.
        if (objectMapper.isResolvable()) {
            ObjectMapperHolder.install(objectMapper.get());
        }

        if (!config.startWorker()) {
            log.info(
                    "workflow worker disabled (workflow.start-worker=false); WorkflowFacade remains"
                            + " usable as a client");
            return;
        }

        NodeHandlerRegistry handlers = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlers);
        int custom = 0;
        for (NodeHandler<?, ?, ?> handler : nodeHandlers) {
            handlers.register(handler); // runs DeterminismGuard.assertPure on the handler class
            log.debug(
                    "registered custom node handler type={} class={}",
                    handler.type(),
                    handler.getClass().getName());
            custom++;
        }

        BusinessActivityRegistry activities =
                activityRegistry.isResolvable()
                        ? activityRegistry.get()
                        : new BusinessActivityRegistry();
        ArchiveActivities archive = archiveActivities.isResolvable() ? archiveActivities.get() : null;
        WorkflowInterceptorRegistry interceptorReg =
                interceptors.isResolvable() ? interceptors.get() : null;

        factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(config.taskQueue());
        WorkerSetup.setup(worker, handlers, activities, archive, interceptorReg);
        factory.start();

        log.info(
                "workflow worker started: task-queue={}, customHandlers={}, archive={},"
                        + " interceptors={}",
                config.taskQueue(),
                custom,
                archive != null,
                interceptorReg != null);
    }

    @PreDestroy
    void shutdown() {
        if (factory != null) {
            factory.shutdown();
        }
    }
}
