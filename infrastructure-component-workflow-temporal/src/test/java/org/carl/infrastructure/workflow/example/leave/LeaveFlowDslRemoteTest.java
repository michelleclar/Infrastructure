package org.carl.infrastructure.workflow.example.leave;

import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;
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
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.handlers.TaskGroupContract;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.BuiltInNodeType;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Opt-in end-to-end test for the flow-first DSL against a real Temporal server. Only runs when
 * {@code TEMPORAL_TARGET} is set, e.g.
 *
 * <pre>
 *   TEMPORAL_TARGET=localhost:7233 \
 *   ./gradlew :infrastructure-component-workflow-temporal:test --rerun-tasks
 * </pre>
 *
 * <p>No time-skipping is available on a real server — only the happy-path (both approve) is covered
 * so the test completes promptly via signals.
 */
@EnabledIfEnvironmentVariable(named = "TEMPORAL_TARGET", matches = ".+")
class LeaveFlowDslRemoteTest {

    private static final String TASK_QUEUE = "leave-v2-dsl-demo-remote-" + UUID.randomUUID();

    private static final String N_REQUEST = "发起请假";
    private static final String N_APPROVAL = "发起请假审批";
    private static final String N_HR = "HR 审批";
    private static final String N_MANAGER = "部门主管审批";
    private static final String N_APPROVED = "休假";
    private static final String N_REJECTED = "拒绝";

    private static final String HR_KEY = TaskGroupContract.childKey(N_APPROVAL, N_HR);
    private static final String MANAGER_KEY = TaskGroupContract.childKey(N_APPROVAL, N_MANAGER);

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
    void coSign_allApproved_runsToCompleted() throws Exception {
        WorkflowDefinition def = buildCoSignFlow();
        GenericWorkflow workflow = newWorkflowStub("dsl-demo-remote-approved-" + UUID.randomUUID());
        WorkflowClient.start(workflow::execute, newInput(def));

        // Signals can be sent immediately on a real server — the workflow will queue them.
        workflow.signal(approvalSignal(HR_KEY, "approved"));
        workflow.signal(approvalSignal(MANAGER_KEY, "approved"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(N_APPROVED, result.finalNodeId());
        assertEquals("APPROVED", result.nodeResults().get(N_APPROVAL).outcome());
        assertNotNull(result.executionRecords(), "executionRecords should be present");
    }

    // ---- flow definition ----------------------------------------------------

    private static WorkflowDefinition buildCoSignFlow() {
        FlowDef flow = Flow.define("leaveCoSignDemoRemote", "请假会签流程 Demo (Remote)");

        flow.start(N_REQUEST);
        flow.node(
                N_REQUEST,
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));

        flow.from(N_REQUEST).on("SUCCESS").to(N_APPROVAL);

        flow.from(N_APPROVAL)
                .join(
                        all(
                                node(N_HR, BuiltInNodes.approval("hr")),
                                node(N_MANAGER, BuiltInNodes.approval("manager"))))
                .on("APPROVED")
                .to(N_APPROVED)
                .on("REJECTED")
                .to(N_REJECTED);

        flow.node(N_APPROVED, b -> b.type(BuiltInNodeType.END_TASK));
        flow.node(N_REJECTED, b -> b.type(BuiltInNodeType.END_TASK));

        return flow.build();
    }

    // ---- helpers ------------------------------------------------------------

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

    private static WorkflowEvent approvalSignal(String taskId, String decision) {
        ObjectNode payload =
                JsonNodeFactory.instance
                        .objectNode()
                        .put("taskId", taskId)
                        .put("decision", decision);
        return new WorkflowEvent("approval", payload);
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
