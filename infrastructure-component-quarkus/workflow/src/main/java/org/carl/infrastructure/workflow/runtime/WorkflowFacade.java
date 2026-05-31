package org.carl.infrastructure.workflow.runtime;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflow.api.Decision;
import org.carl.infrastructure.workflow.api.Vote;

/**
 * Caller-facing facade. Business passes plain POJOs/enums; {@code io.temporal.*} stays here.
 * Uses untyped stubs so no {@code @WorkflowInterface} is required anywhere.
 */

@ApplicationScoped
public class WorkflowFacade {

    @Inject WorkflowClient client;
    @Inject WorkflowConfig config;

    /** Start a process instance. {@code processId} must match a registered {@code ProcessDefinition#id}. */
    public void start(String processId, String bizId, Object ctx) {
        WorkflowStub stub =
                client.newUntypedWorkflowStub(
                        processId,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(config.taskQueue())
                                .setWorkflowId(workflowId(processId, bizId))
                                .build());
        stub.start(ctx);
    }

    /** Deliver an external event (e.g. a human decision) to a running instance. */
    public void signal(String processId, String bizId, Object event) {
        client.newUntypedWorkflowStub(workflowId(processId, bizId)).signal("event", event);
    }

    /**
     * Cast a single vote in a multi-approver gathering state. The engine matches by approver id
     * against the gathering state's assignee set; votes from non-assignees are silently dropped.
     */
    public void vote(String processId, String bizId, String approver, Decision decision) {
        vote(processId, bizId, approver, decision, null);
    }

    /** Vote with a comment. */
    public void vote(
            String processId, String bizId, String approver, Decision decision, String comment) {
        client.newUntypedWorkflowStub(workflowId(processId, bizId))
                .signal("vote", new Vote(approver, decision, comment));
    }

    /** Query the current state of a running instance. */
    public <S> S queryState(String processId, String bizId, Class<S> stateType) {
        return client.newUntypedWorkflowStub(workflowId(processId, bizId))
                .query("state", stateType);
    }

    private static String workflowId(String processId, String bizId) {
        return processId + "-" + bizId;
    }
}
