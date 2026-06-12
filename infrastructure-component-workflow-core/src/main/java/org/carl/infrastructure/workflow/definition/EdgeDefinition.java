package org.carl.infrastructure.workflow.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Definition of a directed edge between two {@link NodeDefinition}s.
 *
 * <p>Field semantics:
 *
 * <ul>
 *   <li>{@code from}: required, source node id.
 *   <li>{@code to}: required, target node id.
 *   <li>{@code event}: optional routing key — the source node's outcome name (the DSL {@code
 *       .on(name)} writes here) or an external event name that activates this edge.
 *   <li>{@code when}: optional EL guard expression (e.g. "${ctx.amount > 10000}").
 * </ul>
 *
 * <p>{@code ignoreUnknown} is set so legacy definitions that still carry the removed {@code
 * outcome} field deserialise cleanly (the field is dropped on read).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EdgeDefinition(String from, String to, String event, String when) {

    public EdgeDefinition {
        Objects.requireNonNull(from, "from");
        if (from.isBlank()) {
            throw new IllegalArgumentException("from must not be blank");
        }
        Objects.requireNonNull(to, "to");
        if (to.isBlank()) {
            throw new IllegalArgumentException("to must not be blank");
        }
        // event, when remain optional / nullable.
    }
}
