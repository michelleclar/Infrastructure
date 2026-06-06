package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Configuration for {@link ServiceTaskHandler}.
 *
 * @param activity activity name resolved by the runtime to actual code (required at runtime).
 * @param activityInput optional structured input passed to the activity.
 * @param compensateActivity optional activity name invoked during saga compensation/rollback. When
 *     {@code null} or blank, compensation is a no-op for this node.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceTaskConfig(
        String activity, Map<String, Object> activityInput, String compensateActivity) {}
