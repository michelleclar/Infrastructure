package org.carl.infrastructure.workflow.runtime;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflow.api.Decision;
import org.carl.infrastructure.workflow.api.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caller-facing facade. Business passes plain POJOs/enums; {@code io.temporal.*} stays here.
 * Uses untyped stubs so no {@code @WorkflowInterface} is required anywhere.
 */

@ApplicationScoped
public class WorkflowFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkflowFacade.class);

    @Inject WorkflowClient client;
    @Inject WorkflowConfig config;

    /** Start a process instance. {@code processId} must match a registered {@code ProcessDefinition#id}. */
    public void start(String processId, String bizId, Object ctx) {
        log.debug("facade.start processId={} workflowId={}", processId, workflowId(processId, bizId));
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
        log.debug("facade.signal workflowId={} event={}", workflowId(processId, bizId), event);
        client.newUntypedWorkflowStub(workflowId(processId, bizId)).signal("event", event);
    }

    /**
     * Cast a single vote on a named step in an approval state.
     *
     * @param step     the step name in the expression tree (e.g. "manager")
     * @param approver the human/system identity casting the vote (informational)
     */
    public void vote(String processId, String bizId, String step, String approver, Decision decision) {
        vote(processId, bizId, step, approver, decision, null);
    }

    /** Vote with a comment. */
    public void vote(
            String processId,
            String bizId,
            String step,
            String approver,
            Decision decision,
            String comment) {
        log.debug("facade.vote workflowId={} step={} approver={} decision={}",
                workflowId(processId, bizId), step, approver, decision);
        client.newUntypedWorkflowStub(workflowId(processId, bizId))
                .signal("vote", new Vote(step, approver, decision, comment));
    }

    /** Query the current state of a running instance. */
    public <S> S queryState(String processId, String bizId, Class<S> stateType) {
        log.trace("facade.queryState workflowId={} type={}", workflowId(processId, bizId), stateType.getSimpleName());
        return client.newUntypedWorkflowStub(workflowId(processId, bizId))
                .query("state", stateType);
    }

    private static String workflowId(String processId, String bizId) {
        return processId + "-" + bizId;
    }
}
