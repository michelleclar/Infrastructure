package org.carl.infrastructure.workflow.testing;

import com.google.protobuf.Duration;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.workflowservice.v1.DescribeNamespaceRequest;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

import org.carl.infrastructure.workflow.api.ProcessDefinition;
import org.carl.infrastructure.workflow.api.ProcessRegistry;
import org.carl.infrastructure.workflow.api.WorkflowExecutionException;
import org.carl.infrastructure.workflow.engine.GenericActivity;
import org.carl.infrastructure.workflow.engine.GenericWorkflow;

/**
 * Temporal-free test driver. This class is the ONLY thing test code needs; it hides all {@code
 * io.temporal.*} (worker registration, stubs, signals, queries, failure mapping) so process tests
 * stay vendor-neutral. Backends: an in-memory time-skipping environment, or a real Temporal server.
 *
 * <p>(Could be promoted to a {@code java-test-fixtures} artifact so other modules' process tests
 * reuse it.)
 */
public final class WorkflowTestKit implements AutoCloseable {

    private final String taskQueue;
    private final WorkflowClient client;
    private final TestWorkflowEnvironment env; // non-null for in-memory
    private final WorkerFactory factory; // non-null for remote
    private final WorkflowServiceStubs serviceStubs; // non-null for remote

    private WorkflowTestKit(
            String taskQueue,
            WorkflowClient client,
            TestWorkflowEnvironment env,
            WorkerFactory factory,
            WorkflowServiceStubs serviceStubs) {
        this.taskQueue = taskQueue;
        this.client = client;
        this.env = env;
        this.factory = factory;
        this.serviceStubs = serviceStubs;
    }

    /** In-memory, time-skipping Temporal (no server, no network). */
    @SafeVarargs
    public static WorkflowTestKit inMemory(String taskQueue, ProcessDefinition<?, ?, ?>... defs) {
        registerAll(defs);
        TestWorkflowEnvironment env = TestWorkflowEnvironment.newInstance();
        registerWorker(env.newWorker(taskQueue));
        env.start();
        return new WorkflowTestKit(taskQueue, env.getWorkflowClient(), env, null, null);
    }

    /** A real Temporal server (e.g. host:port). No time-skipping — avoid long awaits. */
    @SafeVarargs
    public static WorkflowTestKit remote(
            String target, String namespace, String taskQueue, ProcessDefinition<?, ?, ?>... defs) {
        registerAll(defs);
        WorkflowServiceStubs stubs =
                WorkflowServiceStubs.newServiceStubs(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
        ensureNamespace(stubs, namespace);
        WorkflowClient client =
                WorkflowClient.newInstance(
                        stubs, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
        WorkerFactory factory = WorkerFactory.newInstance(client);
        registerWorker(factory.newWorker(taskQueue));
        factory.start();
        return new WorkflowTestKit(taskQueue, client, null, factory, stubs);
    }

    public void start(String processId, String bizId, Object ctx) {
        newStub(processId, bizId).start(ctx);
    }

    public void signal(String processId, String bizId, Object event) {
        existingStub(processId, bizId).signal("event", event);
    }

    public <S> S queryState(String processId, String bizId, Class<S> stateType) {
        return existingStub(processId, bizId).query("state", stateType);
    }

    /** Blocks until the instance finishes; returns the final state, or throws on failure. */
    public <S> S awaitResult(String processId, String bizId, Class<S> stateType) {
        try {
            return existingStub(processId, bizId).getResult(stateType);
        } catch (WorkflowFailedException e) {
            throw new WorkflowExecutionException(
                    "workflow " + workflowId(processId, bizId) + " failed", e);
        }
    }

    @Override
    public void close() {
        if (env != null) {
            env.close();
        }
        if (factory != null) {
            factory.shutdown();
        }
        if (serviceStubs != null) {
            serviceStubs.shutdown();
        }
    }

    /** Creates the namespace if missing (best-effort), then waits for it to become usable. */
    private static void ensureNamespace(WorkflowServiceStubs stubs, String namespace) {
        try {
            stubs.blockingStub()
                    .registerNamespace(
                            RegisterNamespaceRequest.newBuilder()
                                    .setNamespace(namespace)
                                    .setWorkflowExecutionRetentionPeriod(
                                            Duration.newBuilder()
                                                    .setSeconds(3L * 24 * 3600)
                                                    .build())
                                    .build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.ALREADY_EXISTS) {
                throw e;
            }
        }
        // namespace registration propagates asynchronously; wait until it is describable
        for (int i = 0; i < 40; i++) {
            try {
                stubs.blockingStub()
                        .describeNamespace(
                                DescribeNamespaceRequest.newBuilder()
                                        .setNamespace(namespace)
                                        .build());
                return;
            } catch (StatusRuntimeException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static void registerAll(ProcessDefinition<?, ?, ?>[] defs) {
        for (ProcessDefinition<?, ?, ?> def : defs) {
            ProcessRegistry.register(def);
        }
    }

    private static void registerWorker(Worker worker) {
        worker.registerWorkflowImplementationTypes(GenericWorkflow.class);
        worker.registerActivitiesImplementations(new GenericActivity());
    }

    private WorkflowStub newStub(String processId, String bizId) {
        return client.newUntypedWorkflowStub(
                processId,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(taskQueue)
                        .setWorkflowId(workflowId(processId, bizId))
                        .build());
    }

    private WorkflowStub existingStub(String processId, String bizId) {
        return client.newUntypedWorkflowStub(workflowId(processId, bizId));
    }

    private static String workflowId(String processId, String bizId) {
        return processId + "-" + bizId;
    }
}
