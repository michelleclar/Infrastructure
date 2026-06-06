package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Activity input passed by {@link GenericWorkflowImpl} to {@link GenericActivity}.
 *
 * @param activityName name registered through {@link BusinessActivityRegistry}.
 * @param input opaque, JSON-friendly input map; may be {@code null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActivityCall(String activityName, Map<String, Object> input) {}
