package org.carl.infrastructure.workflow.dsl;

import java.util.Objects;

/**
 * Lightweight carrier for a child task inside a {@link JoinSpec}.
 *
 * <p>In the flow-first DSL, child tasks are not registered as top-level workflow nodes; they are
 * embedded in the {@code taskGroup} node's {@code config.tasks} JSON array.
 *
 * <p>Construct via {@link Dsl#node(String, NodeConfig)} (no nested join) or {@link Dsl#node(String,
 * NodeConfig, JoinSpec)} (with a nested join, reserved for the future nested-taskGroup runtime).
 *
 * <p><strong>{@code nestedJoin} field:</strong> reserved field on the POJO layer for nested {@code
 * taskGroup} support. The DSL/POJO layer accepts and round-trips this field, but the runtime layer
 * does not yet honour it; callers should leave it {@code null} for now.
 *
 * @param name the child task name
 * @param config the child node config
 * @param nestedJoin optional nested join spec; {@code null} for leaf children
 */
public record ChildNodeSpec(String name, NodeConfig config, JoinSpec nestedJoin) {

    public ChildNodeSpec {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(config, "config");
        // nestedJoin is intentionally allowed to be null.
    }

    /**
     * Backwards-compatible constructor without a nested join spec. Equivalent to {@code new
     * ChildNodeSpec(name, config, null)}.
     */
    public ChildNodeSpec(String name, NodeConfig config) {
        this(name, config, null);
    }
}
