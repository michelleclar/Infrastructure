package org.carl.infrastructure.workflow.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.spi.NodeSpec;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration carrier for a flow-first DSL node.
 *
 * <p>Holds the node type discriminator (see {@link org.carl.infrastructure.workflow.spi.NodeTypes})
 * and a property map that is serialised into the {@link
 * org.carl.infrastructure.workflow.definition.NodeDefinition#config()} JSON field by {@link
 * FlowDef#build()}.
 *
 * <p>Java DSL code should normally obtain this through {@link NodeBuilder#config(NodeSpec,
 * Object)} or the {@link BuiltInNodes} sugar layer. Direct map construction and {@link
 * #of(NodeSpec, Object)} remain available for low-level and dynamic definitions.
 */
public record NodeConfig(String type, Map<String, Object> props) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECT =
            new TypeReference<>() {};

    public NodeConfig {
        Objects.requireNonNull(type, "type");
        props = props == null ? Map.of() : Map.copyOf(props);
    }

    /**
     * Creates a node config from a strongly typed node spec and its matching config object.
     *
     * <p>The returned instance still stores a map because {@link FlowDef#build()} writes JSON, but
     * callers no longer spell property keys by hand.
     */
    public static <C> NodeConfig of(NodeSpec<C> spec, C config) {
        Objects.requireNonNull(spec, "spec");
        return new NodeConfig(spec.value(), toProps(spec, config));
    }

    static <C> Map<String, Object> toProps(NodeSpec<C> spec, C config) {
        Objects.requireNonNull(spec, "spec");
        if (spec.configType() == Void.class) {
            return Map.of();
        }
        Objects.requireNonNull(config, "config");
        if (!spec.configType().isInstance(config)) {
            throw new IllegalArgumentException(
                    "config type "
                            + config.getClass().getName()
                            + " does not match node spec "
                            + spec.value()
                            + " config type "
                            + spec.configType().getName());
        }
        try {
            Map<String, Object> props = MAPPER.convertValue(config, MAP_OF_OBJECT);
            return props == null ? Map.of() : props;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "config for node spec "
                            + spec.value()
                            + " of type "
                            + config.getClass().getName()
                            + " is not convertible to Map<String, Object>",
                    e);
        }
    }
}
