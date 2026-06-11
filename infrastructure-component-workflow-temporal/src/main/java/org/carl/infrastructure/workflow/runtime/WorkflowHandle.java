package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import io.temporal.client.WorkflowStub;

import org.carl.infrastructure.workflow.spi.WorkflowEvent;

/**
 * A handle to one running workflow instance, returned by {@link WorkflowEngine#start} (or {@link
 * WorkflowEngine#attach}).
 *
 * <p>Business code drives the workflow — signal it, query it, await its result — without importing
 * any {@code io.temporal.*} type; the only Temporal type involved ({@link WorkflowStub}) is hidden
 * behind {@link #awaitResult()}.
 */
public final class WorkflowHandle {

    private final GenericWorkflow stub;
    private final String workflowId;

    WorkflowHandle(GenericWorkflow stub, String workflowId) {
        this.stub = stub;
        this.workflowId = workflowId;
    }

    /** The workflow id this handle addresses. */
    public String workflowId() {
        return workflowId;
    }

    /** Send an event to the workflow (e.g. an approval decision). Fire-and-forget. */
    public void signal(String eventName, JsonNode payload) {
        stub.signal(new WorkflowEvent(eventName, payload));
    }

    /**
     * Send an event carrying an idempotency key — re-sending the same {@code eventId} is de-duplicated
     * by the runtime.
     */
    public void signal(String eventName, JsonNode payload, String eventId) {
        stub.signal(new WorkflowEvent(eventName, payload, eventId));
    }

    /** Send a pre-built event. */
    public void signal(WorkflowEvent event) {
        stub.signal(event);
    }

    /** Read the current workflow state (non-blocking). */
    public WorkflowState query() {
        return stub.query();
    }

    /** Block until the workflow completes and return its result. */
    public WorkflowResult awaitResult() {
        return WorkflowStub.fromTyped(stub).getResult(WorkflowResult.class);
    }
}
