package org.carl.infrastructure.workflow.example.sub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.UUID;

/**
 * End-to-end tests for {@code subProcess} nodes.
 *
 * <p>A parent workflow embeds a child {@link WorkflowDefinition} inline (via {@code
 * SubProcessConfig.definitionJson}). The child is a self-contained V2 workflow that runs on the
 * same task queue; its outcome is mapped back to the parent node outcome by {@code
 * SubProcessConfig.outcomeMapping}.
 */
class SubProcessTest {

    private static final String TASK_QUEUE = "SUB_PROCESS_TEST";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        BusinessActivityRegistry activityRegistry = new BusinessActivityRegistry();
        activityRegistry.register("createRequest", input -> Map.of("requestId", "REQ-001"));
        activityRegistry.register("autoApprove", input -> Map.of("approved", true));
        activityRegistry.register("confirmOrder", input -> Map.of());

        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);

        testEnv.start();
        client = testEnv.getWorkflowClient();
        mapper = ObjectMapperHolder.mapper();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    /**
     * Parent: createRequest → subProcess (auto-approving child) → confirmOrder → completed. Child:
     * autoApprove service task → done (endNode, outcome=COMPLETED). The parent uses {@code
     * outcomeMapping} to map the child's {@code COMPLETED} to {@code approved}, then routes to the
     * confirmOrder step and terminates at {@code completed}.
     */
    @Test
    @Timeout(30)
    void childWorkflow_completesAndParentContinues() throws Exception {
        // --- Child workflow definition ---
        FlowDef childFlow = Flow.define("autoApproveFlow", "Auto-Approve");
        childFlow.start("autoApprove");
        childFlow.node("autoApprove", BuiltInNodes.service("autoApprove"));
        childFlow.node("done", b -> b.type(NodeTypes.END_TASK).label("Done"));
        childFlow.from("autoApprove").on("SUCCESS").to("done");
        WorkflowDefinition childDef = childFlow.build();

        String childDefJson = mapper.writeValueAsString(childDef);

        // --- Parent workflow definition ---
        FlowDef parentFlow = Flow.define("parentOrderFlow", "Parent Order");
        parentFlow.start("createRequest");
        parentFlow.node("createRequest", BuiltInNodes.service("createRequest"));
        parentFlow.node(
                "subApproval",
                new org.carl.infrastructure.workflow.dsl.NodeConfig(
                        NodeTypes.SUB_PROCESS,
                        Map.of(
                                "subWorkflowId",
                                "autoApproveFlow",
                                "definitionJson",
                                childDefJson,
                                "outcomeMapping",
                                Map.of("COMPLETED", "approved"))));
        parentFlow.node("confirmOrder", BuiltInNodes.service("confirmOrder"));
        parentFlow.node("completed", b -> b.type(NodeTypes.END_TASK).label("Completed"));

        parentFlow.from("createRequest").on("SUCCESS").to("subApproval");
        parentFlow.from("subApproval").on("approved").to("confirmOrder");
        parentFlow.from("confirmOrder").on("SUCCESS").to("completed");

        WorkflowDefinition parentDef = parentFlow.build();

        WorkflowInput input = WorkflowInput.from(parentDef, null);

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("sub-process-test-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus());
        assertEquals("completed", result.finalNodeId());

        NodeResult sub = result.nodeResults().get("subApproval");
        assertNotNull(sub, "subApproval result should be recorded");
        assertEquals("approved", sub.outcome(), "outcomeMapping must map COMPLETED → approved");
    }

    /**
     * Verify that a missing {@code definitionJson} causes the parent workflow to fail the
     * subProcess node cleanly (FAILED status, not a runtime exception bubble).
     */
    @Test
    @Timeout(30)
    void missingDefinitionJson_failsNodeCleanly() throws Exception {
        FlowDef parentFlow = Flow.define("badSubFlow", "Bad Sub");
        parentFlow.start("createRequest");
        parentFlow.node("createRequest", BuiltInNodes.service("createRequest"));
        parentFlow.node(
                "sub",
                new org.carl.infrastructure.workflow.dsl.NodeConfig(
                        NodeTypes.SUB_PROCESS,
                        Map.of("subWorkflowId", "missingDef") // intentionally omit definitionJson
                        ));
        parentFlow.node("done", b -> b.type(NodeTypes.END_TASK).label("Done"));

        parentFlow.from("createRequest").on("SUCCESS").to("sub");
        parentFlow.from("sub").on("COMPLETED").to("done");

        WorkflowDefinition parentDef = parentFlow.build();

        WorkflowInput input = WorkflowInput.from(parentDef, null);

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("sub-missing-def-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(
                NodeStatus.FAILED,
                result.finalStatus(),
                "missing definitionJson must result in a FAILED workflow (not an exception)");
        assertEquals("sub", result.finalNodeId());
    }
}
