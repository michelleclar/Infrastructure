package org.carl.infrastructure.workflow.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * External event delivered to a running workflow instance.
 *
 * <p>{@code name} is required and non-blank; {@code payload} is optional.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowEvent(String name, JsonNode payload) {

    public WorkflowEvent {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
