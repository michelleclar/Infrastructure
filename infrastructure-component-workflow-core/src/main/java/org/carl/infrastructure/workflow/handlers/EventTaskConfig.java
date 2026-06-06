package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for {@link EventTaskHandler}.
 *
 * @param awaitedEvent event name to wait for.
 * @param timeoutDuration ISO-8601 timeout, nullable for no timeout.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventTaskConfig(String awaitedEvent, String timeoutDuration) {}
