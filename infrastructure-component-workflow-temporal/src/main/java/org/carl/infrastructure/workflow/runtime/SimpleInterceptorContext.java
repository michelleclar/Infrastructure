package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.interceptor.InterceptorContext;

/**
 * Lightweight {@link InterceptorContext} implementation used both on the workflow side
 * (inline deterministic interceptors) and on the activity side (async interceptors).
 *
 * <p>Implemented as a record for zero-boilerplate immutability; the fields map directly to the
 * {@link InterceptorContext} contract.
 */
public record SimpleInterceptorContext(
        String workflowId,
        String instanceId,
        String definitionId,
        JsonNode businessData) implements InterceptorContext {}
