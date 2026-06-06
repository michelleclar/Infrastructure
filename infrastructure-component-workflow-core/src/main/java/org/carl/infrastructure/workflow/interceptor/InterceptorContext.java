package org.carl.infrastructure.workflow.interceptor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Workflow-level read-only context exposed to interceptors. Node-level data is passed as method
 * parameters on the interceptor hooks themselves.
 */
public interface InterceptorContext {

    /** Temporal workflow instance id. */
    String workflowId();

    /** Temporal run id. */
    String instanceId();

    /** Workflow definition id. */
    String definitionId();

    /** Immutable business input captured at workflow start. */
    JsonNode businessData();
}
