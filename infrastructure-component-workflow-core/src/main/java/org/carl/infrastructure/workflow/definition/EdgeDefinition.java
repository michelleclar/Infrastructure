package org.carl.infrastructure.workflow.definition;

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
 *   <li>{@code event}: optional external event name that activates this edge.
 *   <li>{@code outcome}: optional outcome string from the source node.
 *   <li>{@code when}: optional EL guard expression (e.g. "${ctx.amount > 10000}").
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EdgeDefinition(String from, String to, String event, String outcome, String when) {

    public EdgeDefinition {
        Objects.requireNonNull(from, "from");
        if (from.isBlank()) {
            throw new IllegalArgumentException("from must not be blank");
        }
        Objects.requireNonNull(to, "to");
        if (to.isBlank()) {
            throw new IllegalArgumentException("to must not be blank");
        }
        // event, outcome, when remain optional / nullable.
    }
}
