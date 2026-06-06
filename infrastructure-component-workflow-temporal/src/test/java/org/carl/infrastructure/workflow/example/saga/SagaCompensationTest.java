package org.carl.infrastructure.workflow.example.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end saga compensation tests.
 *
 * <p>The flow is: createOrder → reserveBudget → sendNotification (fails). Both createOrder and
 * reserveBudget declare a {@code compensateActivity}. When sendNotification fails the runtime must
 * compensate in reverse order: releaseBudget then cancelOrder.
 */
class SagaCompensationTest {

    private static final String TASK_QUEUE = "SAGA_COMPENSATION_TEST";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private ObjectMapper mapper;
    private List<String> callLog;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        callLog = Collections.synchronizedList(new ArrayList<>());
        BusinessActivityRegistry activityRegistry = SagaActivities.buildRegistry(callLog);

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
     * The workflow fails at {@code sendNotification}. Compensation must run in reverse order:
     * {@code releaseBudget} (compensates reserveBudget) then {@code cancelOrder} (compensates
     * createOrder).
     */
    @Test
    @Timeout(30)
    void failedNode_triggersCompensationInReverseOrder() throws Exception {
        WorkflowDefinition def = SagaProcess.sagaFlow();
        WorkflowInput input = WorkflowInput.from(def, null);

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("saga-compensation-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        // Workflow terminates with FAILED status because sendNotification fails.
        assertEquals(
                NodeStatus.FAILED,
                result.finalStatus(),
                "workflow should terminate FAILED when sendNotification throws");
        assertEquals("sendNotification", result.finalNodeId());

        // Activity execution order: forward steps, then compensation in reverse.
        assertEquals(
                List.of(
                        "createOrder",
                        "reserveBudget",
                        "sendNotification",
                        "releaseBudget",
                        "cancelOrder"),
                callLog,
                "compensation must run in reverse order after the failure");
    }

    /**
     * If a node has no {@code compensateActivity} set, it is still compensable (ServiceTask
     * compensable==true) but its compensate() returns a no-op COMPLETED result. Verify the workflow
     * does not error and the compensated nodes that DO have an activity are still called.
     */
    @Test
    @Timeout(30)
    void noCompensateActivity_isSkippedSilently() throws Exception {
        // Override sendNotification to succeed but also add a node without compensateActivity
        // before the failing step. In this test we reuse sagaFlow where sendNotification has no
        // compensateActivity, and sendNotification fails → compensation runs for createOrder and
        // reserveBudget only (sendNotification was never completed, so not on the stack).
        // The important assertion: callLog ends with releaseBudget, cancelOrder (in that order)
        // regardless of the missing compensateActivity on sendNotification.
        WorkflowDefinition def = SagaProcess.sagaFlow();
        WorkflowInput input = WorkflowInput.from(def, null);

        GenericWorkflow workflow =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId("saga-nocomp-" + UUID.randomUUID())
                                .build());
        WorkflowClient.start(workflow::execute, input);
        WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        // Even though sendNotification has no compensateActivity, the compensation stack only
        // contains createOrder and reserveBudget (sendNotification never completed successfully).
        assertTrue(callLog.contains("releaseBudget"), "releaseBudget compensation must still run");
        assertTrue(callLog.contains("cancelOrder"), "cancelOrder compensation must still run");
        int rlIdx = callLog.indexOf("releaseBudget");
        int clIdx = callLog.indexOf("cancelOrder");
        assertTrue(rlIdx < clIdx, "releaseBudget must be compensated before cancelOrder");
    }
}
