package org.carl.infrastructure.workflow.quarkus;

import com.fasterxml.jackson.databind.JsonNode;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.runtime.GenericWorkflow;
import org.carl.infrastructure.workflow.runtime.WorkflowInput;
import org.carl.infrastructure.workflow.runtime.WorkflowResult;
import org.carl.infrastructure.workflow.runtime.WorkflowState;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Caller-facing facade. Business code passes plain POJOs / {@link WorkflowDefinition} / Jackson
 * {@link JsonNode}; every {@code io.temporal.*} type stays inside this class. Uses the engine's typed
 * {@link GenericWorkflow} stub so no signal/query method names are referenced by string.
 *
 * <p>The {@code workflowId} is the caller's idempotency key for an instance (e.g. {@code
 * "leave-" + bizId}); reuse it across {@link #signal}, {@link #query} and {@link #awaitResult} to
 * address the same running workflow.
 */
@ApplicationScoped
public class WorkflowFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkflowFacade.class);

    @Inject WorkflowClient client;
    @Inject WorkflowConfig config;

    /**
     * Start a workflow instance from a {@link WorkflowDefinition}. Fire-and-forget; returns the
     * {@code workflowId} for subsequent signal/query/await calls.
     */
    public String start(
            WorkflowDefinition definition, String workflowId, Map<String, Object> businessData) {
        return start(definition, workflowId, businessData, false);
    }

    /** As {@link #start(WorkflowDefinition, String, Map)} with an explicit archival opt-in. */
    public String start(
            WorkflowDefinition definition,
            String workflowId,
            Map<String, Object> businessData,
            boolean archive) {
        log.debug("facade.start workflowId={} definition={}", workflowId, definition.id());
        GenericWorkflow stub =
                client.newWorkflowStub(
                        GenericWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(config.taskQueue())
                                .setWorkflowId(workflowId)
                                .build());
        WorkflowInput input = WorkflowInput.from(definition, businessData).withArchive(archive);
        WorkflowClient.start(stub::execute, input);
        return workflowId;
    }

    /**
     * Deliver an external event (e.g. an approval decision) to a running instance. {@code eventName}
     * is the event the target node awaits (e.g. {@code "approval"}); {@code payload} is the event
     * body (e.g. {@code {"decision":"approved"}}).
     */
    public void signal(String workflowId, String eventName, JsonNode payload) {
        log.debug("facade.signal workflowId={} event={}", workflowId, eventName);
        client.newWorkflowStub(GenericWorkflow.class, workflowId)
                .signal(new WorkflowEvent(eventName, payload));
    }

    /**
     * Signal with an idempotency key — re-sending the same {@code eventId} is de-duplicated by the
     * runtime (network retries, duplicate submissions).
     */
    public void signal(String workflowId, String eventName, JsonNode payload, String eventId) {
        log.debug("facade.signal workflowId={} event={} eventId={}", workflowId, eventName, eventId);
        client.newWorkflowStub(GenericWorkflow.class, workflowId)
                .signal(new WorkflowEvent(eventName, payload, eventId));
    }

    /** Query the current state of a running instance (non-blocking). */
    public WorkflowState query(String workflowId) {
        log.trace("facade.query workflowId={}", workflowId);
        return client.newWorkflowStub(GenericWorkflow.class, workflowId).query();
    }

    /** Block until the workflow terminates and return its result. */
    public WorkflowResult awaitResult(String workflowId) {
        log.trace("facade.awaitResult workflowId={}", workflowId);
        GenericWorkflow stub = client.newWorkflowStub(GenericWorkflow.class, workflowId);
        return WorkflowStub.fromTyped(stub).getResult(WorkflowResult.class);
    }
}
