package org.carl.infrastructure.workflow.example.leave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.Duration;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.workflowservice.v1.DescribeNamespaceRequest;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.UUID;

/**
 * End-to-end V2 tests against a real Temporal server. Opt-in: only runs when {@code
 * TEMPORAL_TARGET} is set, e.g.
 *
 * <pre>
 *   TEMPORAL_TARGET=180.184.66.147:31733 \
 *   ./gradlew :infrastructure-component-workflow-temporal:test --rerun-tasks
 * </pre>
 *
 * No time-skipping on a real server — only signal-driven paths are covered.
 */
@EnabledIfEnvironmentVariable(named = "TEMPORAL_TARGET", matches = ".+")
class LeaveWorkflowRemoteTest {

    private static final String TASK_QUEUE = "leave-v2-remote-" + UUID.randomUUID();

    private WorkflowServiceStubs stubs;
    private WorkerFactory factory;
    private WorkflowClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        String target = System.getenv("TEMPORAL_TARGET");
        String namespace = System.getenv().getOrDefault("TEMPORAL_NAMESPACE", "default");

        stubs =
                WorkflowServiceStubs.newServiceStubs(
                        WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
        ensureNamespace(stubs, namespace);

        client =
                WorkflowClient.newInstance(
                        stubs, WorkflowClientOptions.newBuilder().setNamespace(namespace).build());

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);

        BusinessActivityRegistry activityRegistry = LeaveActivities.buildRegistry();

        factory = WorkerFactory.newInstance(client);
        WorkerSetup.setup(factory.newWorker(TASK_QUEUE), handlerRegistry, activityRegistry);
        factory.start();

        mapper = ObjectMapperHolder.mapper();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) factory.shutdown();
        if (stubs != null) stubs.shutdown();
    }

    @Test
    void approved_runsToCompleted() throws Exception {
        WorkflowDefinition def = LeaveProcess.singleApprovalFlow();
        GenericWorkflow workflow = newWorkflowStub("leave-remote-approved-" + UUID.randomUUID());
        WorkflowClient.start(workflow::execute, newInput(def));

        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals("completed", result.finalNodeId());
        assertEquals(Outcomes.APPROVED, result.nodeResults().get("leaveApproval").outcome());
        assertNotNull(result.executionRecords(), "executionRecords should be present");
    }

    @Test
    void rejected_routesToRejectedEnd() throws Exception {
        WorkflowDefinition def = LeaveProcess.singleApprovalFlow();
        GenericWorkflow workflow = newWorkflowStub("leave-remote-rejected-" + UUID.randomUUID());
        WorkflowClient.start(workflow::execute, newInput(def));

        workflow.signal(new WorkflowEvent("approval", decisionPayload("rejected")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals("rejected", result.finalNodeId());
        assertEquals(Outcomes.REJECTED, result.nodeResults().get("leaveApproval").outcome());
    }

    // ---- helpers ----------------------------------------------------------------

    private WorkflowInput newInput(WorkflowDefinition def) throws Exception {
        return WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));
    }

    private GenericWorkflow newWorkflowStub(String workflowId) {
        return client.newWorkflowStub(
                GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());
    }

    private static ObjectNode decisionPayload(String decision) {
        return JsonNodeFactory.instance.objectNode().put("decision", decision);
    }

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
            if (e.getStatus().getCode() != Status.Code.ALREADY_EXISTS) throw e;
        }
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
}
