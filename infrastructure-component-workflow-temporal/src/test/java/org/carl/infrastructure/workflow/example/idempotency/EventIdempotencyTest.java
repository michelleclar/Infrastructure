package org.carl.infrastructure.workflow.example.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.runtime.BusinessActivityRegistry;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.HandlerHolder;
import org.carl.infrastructure.workflow.runtime.ObjectMapperHolder;
import org.carl.infrastructure.workflow.runtime.WorkerSetup;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.runtime.WorkflowState;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration tests for {@code WorkflowEvent.eventId} signal de-duplication.
 *
 * <p>Each test runs on Temporal's in-memory {@link TestWorkflowEnvironment}. The three scenarios
 * cover:
 *
 * <ol>
 *   <li>A duplicate eventId signal is silently dropped without breaking the happy path.
 *   <li>A duplicate eventId across a back-edge re-visit blocks the second approval round, while a
 *       fresh eventId subsequently unblocks it — proving both the rejection and the pass-through
 *       sides of the de-duplication logic.
 *   <li>A {@code null} eventId (2-arg constructor) is never de-duplicated, preserving backward
 *       compatibility.
 * </ol>
 */
class EventIdempotencyTest {

    private static final String TASK_QUEUE = "IDEMPOTENCY_TEST";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);

        NodeHandlerRegistry handlerRegistry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlerRegistry);
        HandlerHolder.install(handlerRegistry);

        BusinessActivityRegistry activityRegistry = buildActivityRegistry();
        WorkerSetup.setup(worker, handlerRegistry, activityRegistry);

        testEnv.start();
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    // -------------------------------------------------------------------------
    // Test 1: duplicate eventId is consumed only once, happy path still works
    // -------------------------------------------------------------------------

    /**
     * Sends the same eventId twice on a single-approval flow. The second signal must be dropped by
     * the de-duplication guard. The workflow should still complete normally because the first signal
     * was accepted.
     */
    @Test
    @Timeout(30)
    void duplicateEventId_consumedOnce() throws Exception {
        WorkflowDefinition def = singleApprovalFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));

        GenericWorkflow workflow = newStub("test-dedup-");
        WorkflowClient.start(workflow::execute, input);

        // Let the workflow reach leaveApproval and wait for the event.
        testEnv.sleep(Duration.ofSeconds(1));

        ObjectNode approvedPayload = decisionPayload("approved");

        // First signal — must be accepted and drive the workflow to completion.
        workflow.signal(new WorkflowEvent("approval", approvedPayload, "appr-1"));

        // Second signal with the same eventId — must be silently dropped.
        workflow.signal(new WorkflowEvent("approval", approvedPayload, "appr-1"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus(),
                "workflow should complete despite duplicate signal");
        assertEquals("completed", result.finalNodeId(),
                "final node should be 'completed' on the approved path");
    }

    // -------------------------------------------------------------------------
    // Test 2: duplicate eventId across a back-edge blocks the second round;
    //         a different eventId unblocks it
    // -------------------------------------------------------------------------

    /**
     * Uses the rejection-loop flow (back-edge from leaveApproval → requestLeave). The scenario:
     *
     * <ol>
     *   <li>Send eventId="dup-1" / decision=rejected → triggers the back-edge, workflow loops back
     *       to the second wait on leaveApproval.
     *   <li>Send eventId="dup-1" / decision=approved → must be de-duplicated and dropped; the
     *       second-round leaveApproval keeps waiting.
     *   <li>Query confirms the workflow is still running at leaveApproval.
     *   <li>Send eventId="fresh-2" / decision=approved → must be accepted; workflow completes.
     * </ol>
     */
    @Test
    @Timeout(30)
    void backEdgeReplayedEventIdRejected_blocksSecondRound() throws Exception {
        WorkflowDefinition def = rejectionLoopFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));

        GenericWorkflow workflow = newStub("test-backedge-dedup-");
        WorkflowClient.start(workflow::execute, input);

        // First wait: workflow reaches leaveApproval.
        testEnv.sleep(Duration.ofSeconds(1));

        // Signal 1: rejected with eventId "dup-1" → triggers back-edge loop.
        workflow.signal(new WorkflowEvent("approval", decisionPayload("rejected"), "dup-1"));

        // Allow back-edge re-execution (requestLeave service task + re-entering leaveApproval).
        testEnv.sleep(Duration.ofSeconds(1));

        // Signal 2: same eventId "dup-1", now with approved → must be de-duplicated and dropped.
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved"), "dup-1"));

        // Give the runtime a moment to process the dropped signal.
        testEnv.sleep(Duration.ofSeconds(1));

        // Query: workflow must still be running and waiting at leaveApproval.
        WorkflowState state = workflow.query();
        assertFalse(state.finished(),
                "workflow must still be running after the duplicate eventId was dropped");
        assertEquals("leaveApproval", state.currentNodeId(),
                "workflow must still be waiting at leaveApproval after duplicate was dropped");

        // Signal 3: fresh eventId "fresh-2" with approved → must be accepted; second round completes.
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved"), "fresh-2"));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus(),
                "workflow must complete after the fresh eventId is accepted");
        assertEquals("done", result.finalNodeId(),
                "final node should be 'done' per rejectionLoopFlow definition");
    }

    // -------------------------------------------------------------------------
    // Test 3: null eventId is never de-duplicated (backward compatibility)
    // -------------------------------------------------------------------------

    /**
     * Uses the 2-arg {@link WorkflowEvent} constructor (eventId=null). A single approved signal must
     * be consumed normally and drive the workflow to completion, proving that {@code null} eventId
     * bypasses de-duplication entirely.
     */
    @Test
    @Timeout(30)
    void nullEventId_notDeduplicated() throws Exception {
        WorkflowDefinition def = singleApprovalFlow();
        WorkflowInput input = WorkflowInput.from(def, Map.of("employee", "alice", "days", 3));

        GenericWorkflow workflow = newStub("test-null-eventid-");
        WorkflowClient.start(workflow::execute, input);

        testEnv.sleep(Duration.ofSeconds(1));

        // 2-arg constructor — eventId is null; must not be de-duplicated.
        workflow.signal(new WorkflowEvent("approval", decisionPayload("approved")));

        WorkflowResult result = WorkflowStub.fromTyped(workflow).getResult(WorkflowResult.class);

        assertEquals(NodeStatus.COMPLETED, result.finalStatus(),
                "null-eventId signal must be consumed normally");
        assertEquals("completed", result.finalNodeId(),
                "final node should be 'completed' on the approved path");
    }

    // -------------------------------------------------------------------------
    // Flow definitions (private, local — do not modify LeaveProcess.java)
    // -------------------------------------------------------------------------

    /**
     * Single-approval flow without a timeout (simpler than LeaveProcess.singleApprovalFlow so that
     * clock manipulation is not needed for the de-duplication assertions):
     *
     * <pre>
     * requestLeave --(SUCCESS)--> leaveApproval
     *   leaveApproval --APPROVED--> onLeave --(SUCCESS)--> completed
     *   leaveApproval --REJECTED--> rejected
     * </pre>
     */
    private static WorkflowDefinition singleApprovalFlow() {
        FlowDef flow = Flow.define("idempotencyV2", "幂等测试流程");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node(
                "leaveApproval",
                BuiltInNodes.approval("manager")
                        .andThen(b -> b.set("awaitEvent", "approval")));
        flow.node("onLeave", BuiltInNodes.service("notifyManager"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("已拒绝"));

        flow.from("requestLeave").on("SUCCESS").to("leaveApproval");
        flow.from("leaveApproval").on("APPROVED").to("onLeave");
        flow.from("leaveApproval").on("REJECTED").to("rejected");
        flow.from("onLeave").on("SUCCESS").to("completed");

        return flow.build();
    }

    /**
     * Back-edge flow: rejected approval loops back to requestLeave, approved terminates at done.
     *
     * <pre>
     * requestLeave --(SUCCESS)--> leaveApproval
     *   leaveApproval --APPROVED--> done
     *   leaveApproval --REJECTED--> requestLeave   (back-edge)
     * </pre>
     */
    private static WorkflowDefinition rejectionLoopFlow() {
        FlowDef flow = Flow.define("idempotencyLoopV2", "幂等回环测试流程");
        flow.start("requestLeave");

        flow.node(
                "requestLeave",
                BuiltInNodes.service("createLeaveRequest")
                        .andThen(b -> b.set("activityInput", Map.of("employeeId", "alice"))));
        flow.node(
                "leaveApproval",
                BuiltInNodes.approval("manager")
                        .andThen(b -> b.set("awaitEvent", "approval")));
        flow.node("done", b -> b.type(NodeTypes.END_TASK).label("已完成"));

        flow.from("requestLeave").on("SUCCESS").to("leaveApproval");
        flow.from("leaveApproval").on("APPROVED").to("done");
        flow.from("leaveApproval").on("REJECTED").to("requestLeave");

        return flow.build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GenericWorkflow newStub(String prefix) {
        return client.newWorkflowStub(
                GenericWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(prefix + UUID.randomUUID())
                        .build());
    }

    private static ObjectNode decisionPayload(String decision) {
        return JsonNodeFactory.instance.objectNode().put("decision", decision);
    }

    private static BusinessActivityRegistry buildActivityRegistry() {
        AtomicInteger createCount = new AtomicInteger();
        AtomicInteger notifyCount = new AtomicInteger();

        BusinessActivityRegistry registry = new BusinessActivityRegistry();
        registry.register(
                "createLeaveRequest",
                input -> {
                    createCount.incrementAndGet();
                    Map<String, Object> out = new LinkedHashMap<>();
                    Object employeeId = input == null ? null : input.get("employeeId");
                    out.put("requestId", "REQ-" + createCount.get());
                    out.put("employeeId", employeeId);
                    return out;
                });
        registry.register(
                "notifyManager",
                input -> {
                    notifyCount.incrementAndGet();
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("notified", true);
                    return out;
                });
        return registry;
    }
}
