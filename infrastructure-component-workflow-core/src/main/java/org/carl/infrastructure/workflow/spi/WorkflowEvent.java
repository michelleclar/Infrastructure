package org.carl.infrastructure.workflow.spi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * External event delivered to a running workflow instance.
 *
 * <p>{@code name} is required and non-blank; {@code payload} is optional. {@code eventId} is an
 * optional idempotency key: when non-null and non-blank it is used by the signal handler to
 * de-duplicate repeated signal deliveries (network retries, duplicate client submissions, etc.).
 * A {@code null} {@code eventId} means the event does not participate in de-duplication, which
 * preserves full backward compatibility with existing callers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowEvent(String name, JsonNode payload, String eventId) {

    public WorkflowEvent {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        // eventId is nullable: a null/blank eventId means the event does not participate in
        // signal de-duplication (backward-compatible default).
    }

    /**
     * Backward-compatible constructor without an idempotency key. {@code eventId} defaults to
     * {@code null}, so the event does not participate in signal de-duplication.
     */
    public WorkflowEvent(String name, JsonNode payload) {
        this(name, payload, null);
    }
}
