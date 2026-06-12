package org.carl.infrastructure.workflow.spi;

import java.util.Objects;

/**
 * Strongly typed node declaration used by the Java DSL.
 *
 * <p>{@link NodeType} only protects the node type discriminator string. {@code NodeSpec<C>} also
 * binds that discriminator to the handler configuration class, so DSL calls can require the exact
 * config record at compile time while the persisted {@code WorkflowDefinition} remains plain JSON.
 *
 * @param <C> handler configuration type
 */
public record NodeSpec<C>(String value, Class<C> configType) implements NodeType {

    public NodeSpec {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        Objects.requireNonNull(configType, "configType");
    }

    public static <C> NodeSpec<C> of(String value, Class<C> configType) {
        return new NodeSpec<>(value, configType);
    }
}
