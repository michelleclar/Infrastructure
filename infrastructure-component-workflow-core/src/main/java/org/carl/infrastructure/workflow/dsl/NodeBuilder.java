package org.carl.infrastructure.workflow.dsl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable builder used by {@link FlowDef#node(String, java.util.function.Consumer)} to compose a
 * {@link NodeConfig} via a fluent, type-agnostic API.
 *
 * <p>The builder is type-agnostic: any node type string accepted by a registered {@link
 * org.carl.infrastructure.workflow.spi.NodeHandler} can be set via {@link #type(String)}, including
 * business-defined types that {@code workflow-core} has no knowledge of. This is the extensibility
 * seam that allows custom handlers to participate in the DSL without modifying any code in {@code
 * workflow-core}.
 *
 * <p>The optional {@link #label(String)} value is propagated to {@link
 * org.carl.infrastructure.workflow.definition.NodeDefinition#label()} when the parent {@link
 * FlowDef#build()} runs; if absent, the node id is used as the label (existing behaviour).
 */
public final class NodeBuilder {

    private String type;
    private String label;
    private final Map<String, Object> props = new LinkedHashMap<>();

    /**
     * Sets the node type discriminator (e.g. {@code "serviceTask"} or any business-defined string).
     */
    public NodeBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets an optional display label for the node. When absent, the node id is used as the label.
     */
    public NodeBuilder label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Stores a single configuration property. Later calls with the same key overwrite earlier
     * values.
     */
    public NodeBuilder set(String key, Object value) {
        this.props.put(Objects.requireNonNull(key, "key"), value);
        return this;
    }

    /**
     * Bulk-copies the supplied entries into the configuration property map. {@code null} input is
     * tolerated and treated as an empty map.
     */
    public NodeBuilder config(Map<String, Object> values) {
        if (values != null) {
            this.props.putAll(values);
        }
        return this;
    }

    // ----- package-private accessors used by FlowDef -----

    String getType() {
        return type;
    }

    String getLabel() {
        return label;
    }

    Map<String, Object> getProps() {
        return props;
    }

    /**
     * Compiles the accumulated state into an immutable {@link NodeConfig}.
     *
     * @throws IllegalStateException if {@link #type(String)} was never called or was blank
     */
    NodeConfig buildConfig() {
        if (type == null || type.isBlank()) {
            throw new IllegalStateException("NodeBuilder.type(...) is required");
        }
        return new NodeConfig(type, Map.copyOf(props));
    }
}
