package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for {@link UserTaskHandler}.
 *
 * @param assignee assignee identifier or expression.
 * @param awaitEvent event name that completes the task (defaults to {@code "userTask"} when {@code
 *     null} or blank).
 * @param timeoutDuration ISO-8601 timeout, nullable for no timeout.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserTaskConfig(String assignee, String awaitEvent, String timeoutDuration) {}
