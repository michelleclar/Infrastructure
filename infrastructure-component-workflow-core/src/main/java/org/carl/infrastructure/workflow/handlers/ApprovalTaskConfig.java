package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for {@link ApprovalTaskHandler}.
 *
 * @param assignee assignee identifier or expression.
 * @param awaitEvent event name that completes the task (defaults to {@code "approval"} when {@code
 *     null} or blank).
 * @param timeoutDuration ISO-8601 timeout (e.g. {@code "PT24H"}), nullable for no timeout.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApprovalTaskConfig(String assignee, String awaitEvent, String timeoutDuration) {}
