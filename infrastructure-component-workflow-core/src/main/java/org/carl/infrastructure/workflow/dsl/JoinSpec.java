package org.carl.infrastructure.workflow.dsl;

import java.util.List;
import java.util.Objects;

/**
 * Describes the join policy and child tasks for a {@code taskGroup} node in the flow-first DSL.
 *
 * <p>{@code rule} is either {@code "all"} (all children must complete) or {@code "any"} (first
 * completing child short-circuits the group).
 *
 * <p>Construct via {@link Dsl#all(ChildNodeSpec...)} or {@link Dsl#any(ChildNodeSpec...)}.
 */
public record JoinSpec(String rule, List<ChildNodeSpec> children) {

    public JoinSpec {
        Objects.requireNonNull(rule, "rule");
        if (!rule.equals("all") && !rule.equals("any")) {
            throw new IllegalArgumentException("rule must be 'all' or 'any', got: " + rule);
        }
        Objects.requireNonNull(children, "children");
        if (children.isEmpty()) {
            throw new IllegalArgumentException("JoinSpec must have at least one child");
        }
        children = List.copyOf(children);
    }
}
