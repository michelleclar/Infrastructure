package org.carl.infrastructure.workflow.runtime;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import org.carl.infrastructure.workflow.archive.ArchiveActivities;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.interceptor.WorkflowInterceptorRegistry;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Business-facing facade over the Temporal workflow runtime.
 *
 * <p>Everything Temporal — service stubs, client, worker factory, typed stubs, workflow options — is
 * hidden behind this class and {@link WorkflowHandle}. Business code wires the engine, runs workflows
 * and drives them with <strong>zero {@code io.temporal.*} imports</strong>:
 *
 * <pre>{@code
 * try (WorkflowEngine engine =
 *         WorkflowEngine.connect(EngineConfig.of("localhost:7233", "MY_QUEUE"))
 *                 .withWorker(handlers, activities)) {
 *     WorkflowHandle h = engine.start(definition, Map.of("employee", "alice"));
 *     h.signal("approval", approvedPayload);
 *     WorkflowResult result = h.awaitResult();
 * }
 * }</pre>
 *
 * <p>One engine owns one worker on one task queue. Call a {@code withWorker(...)} overload once to
 * register handlers/activities (and optionally archival + interceptors) and start polling; or skip
 * it and use {@link #attach(String)} for a client-only engine that just drives existing workflows.
 */
public final class WorkflowEngine implements AutoCloseable {

    private final WorkflowServiceStubs service;
    private final WorkflowClient client;
    private final WorkerFactory factory;
    private final String taskQueue;
    private boolean workerStarted;

    private WorkflowEngine(
            WorkflowServiceStubs service,
            WorkflowClient client,
            WorkerFactory factory,
            String taskQueue) {
        this.service = service;
        this.client = client;
        this.factory = factory;
        this.taskQueue = taskQueue;
    }

    /** Connect to Temporal using {@code config}. Does not start a worker — call {@code withWorker}. */
    public static WorkflowEngine connect(EngineConfig config) {
        Objects.requireNonNull(config, "config");
        WorkflowServiceStubs service =
                WorkflowServiceStubs.newInstance(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(config.target()).build());
        WorkflowClient client =
                WorkflowClient.newInstance(
                        service,
                        WorkflowClientOptions.newBuilder()
                                .setNamespace(config.namespace())
                                .build());
        WorkerFactory factory = WorkerFactory.newInstance(client);
        return new WorkflowEngine(service, client, factory, config.taskQueue());
    }

    // ── worker registration ───────────────────────────────────────────────────────────────────

    /** Register handlers + activities on this engine's task queue and start polling. */
    public WorkflowEngine withWorker(
            NodeHandlerRegistry handlers, BusinessActivityRegistry activities) {
        return withWorker(handlers, activities, null, null);
    }

    /** As {@link #withWorker(NodeHandlerRegistry, BusinessActivityRegistry)} plus archival. */
    public WorkflowEngine withWorker(
            NodeHandlerRegistry handlers,
            BusinessActivityRegistry activities,
            ArchiveActivities archive) {
        return withWorker(handlers, activities, archive, null);
    }

    /** Full registration: handlers + activities + optional archival + optional interceptors. */
    public WorkflowEngine withWorker(
            NodeHandlerRegistry handlers,
            BusinessActivityRegistry activities,
            ArchiveActivities archive,
            WorkflowInterceptorRegistry interceptors) {
        Worker worker = factory.newWorker(taskQueue);
        WorkerSetup.setup(worker, handlers, activities, archive, interceptors);
        factory.start();
        workerStarted = true;
        return this;
    }

    // ── run / drive workflows ──────────────────────────────────────────────────────────────────

    /** Start a workflow with an auto-generated id ({@code <definitionId>-<uuid>}), no archival. */
    public WorkflowHandle start(WorkflowDefinition definition, Map<String, Object> businessData) {
        return start(definition, businessData, definition.id() + "-" + UUID.randomUUID(), false);
    }

    /** Start a workflow with an explicit id and archival opt-in. */
    public WorkflowHandle start(
            WorkflowDefinition definition,
            Map<String, Object> businessData,
            String workflowId,
            boolean archive) {
        GenericWorkflow stub =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(taskQueue)
                                .setWorkflowId(workflowId)
                                .build());
        WorkflowInput input = WorkflowInput.from(definition, businessData).withArchive(archive);
        WorkflowClient.start(stub::execute, input);
        return new WorkflowHandle(stub, workflowId);
    }

    /** Attach to an already-running workflow by id (to signal / query / await it). */
    public WorkflowHandle attach(String workflowId) {
        GenericWorkflow stub = client.newWorkflowStub(GenericWorkflow.class, workflowId);
        return new WorkflowHandle(stub, workflowId);
    }

    @Override
    public void close() {
        if (workerStarted) {
            factory.shutdown();
        }
        service.shutdown();
    }
}
