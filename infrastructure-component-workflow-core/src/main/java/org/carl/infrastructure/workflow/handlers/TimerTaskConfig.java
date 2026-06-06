package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for {@link TimerTaskHandler}.
 *
 * @param duration ISO-8601 duration (e.g. {@code "PT5M"}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TimerTaskConfig(String duration) {}
