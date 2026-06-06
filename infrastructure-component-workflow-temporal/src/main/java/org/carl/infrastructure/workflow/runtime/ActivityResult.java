package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Activity output returned by {@link GenericActivity} to {@link GenericWorkflowImpl}.
 *
 * @param success whether the underlying business function ran to completion without throwing.
 * @param output structured output (defaults to empty map on failure or {@code null}).
 * @param error error message when {@code success} is false; {@code null} otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActivityResult(boolean success, Map<String, Object> output, String error) {}
