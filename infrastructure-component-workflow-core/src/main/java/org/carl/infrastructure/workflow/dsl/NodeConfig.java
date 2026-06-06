package org.carl.infrastructure.workflow.dsl;

import java.util.Map;
import java.util.Objects;

/**
 * Typed configuration carrier for a flow-first DSL node.
 *
 * <p>Holds the node type discriminator (see {@link org.carl.infrastructure.workflow.spi.NodeTypes})
 * and a property map that is serialised into the {@link
 * org.carl.infrastructure.workflow.definition.NodeDefinition#config()} JSON field by {@link
 * FlowDef#build()}.
 *
 * <p>Typically obtained via {@link NodeBuilder#buildConfig()} — either through {@link
 * FlowDef#node(String, java.util.function.Consumer)} or the {@link BuiltInNodes} sugar layer.
 */
public record NodeConfig(String type, Map<String, Object> props) {

    public NodeConfig {
        Objects.requireNonNull(type, "type");
        props = props == null ? Map.of() : Map.copyOf(props);
    }
}
