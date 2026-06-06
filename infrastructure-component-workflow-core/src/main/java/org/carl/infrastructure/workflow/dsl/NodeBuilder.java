package org.carl.infrastructure.workflow.dsl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.spi.NodeType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable builder used by {@link FlowDef#node(String, java.util.function.Consumer)} to compose a
 * {@link NodeConfig} via a fluent, type-agnostic API.
 *
 * <p>The builder is type-agnostic: any node type accepted by a registered {@link
 * org.carl.infrastructure.workflow.spi.NodeHandler} can be set via {@link #type(String)} or its
 * typed overload {@link #type(NodeType)}, including business-defined types that
 * {@code workflow-core} has no knowledge of. This is the extensibility seam that allows custom
 * handlers to participate in the DSL without modifying any code in {@code workflow-core}.
 *
 * <p>Configuration values can be supplied either as individual {@code (key, value)} pairs via
 * {@link #set(String, Object)}, in bulk via {@link #setAll(Map)}, or directly from a typed POJO
 * via {@link #setAll(Object)}. The POJO form delegates to Jackson's
 * {@link ObjectMapper#convertValue(Object, TypeReference) convertValue} so any record / bean
 * Jackson can serialise is accepted; this is the recommended path for custom handlers that
 * already declare a typed {@code Config} record.
 *
 * <p>The optional {@link #label(String)} value is propagated to {@link
 * org.carl.infrastructure.workflow.definition.NodeDefinition#label()} when the parent {@link
 * FlowDef#build()} runs; if absent, the node id is used as the label.
 */
public final class NodeBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECT =
            new TypeReference<>() {};

    private String type;
    private String label;
    private final Map<String, Object> props = new LinkedHashMap<>();

    /**
     * Sets the node type discriminator from a raw string (e.g. {@code "serviceTask"} or any
     * business-defined value).
     */
    public NodeBuilder type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Typed overload that accepts a {@link NodeType} (e.g.
     * {@code BuiltInNodeType.SERVICE_TASK} or a business-defined enum). Internally stores
     * {@link NodeType#value()} so the JSON wire format remains a plain string.
     */
    public NodeBuilder type(NodeType nodeType) {
        return type(nodeType == null ? null : nodeType.value());
    }

    /** Sets an optional display label for the node. When absent, the node id is used as the label. */
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
    public NodeBuilder setAll(Map<String, Object> values) {
        if (values != null) {
            this.props.putAll(values);
        }
        return this;
    }

    /**
     * Typed-POJO overload: converts the supplied object to a {@code Map<String, Object>} via
     * Jackson and merges the entries into the configuration property map. {@code null} input is
     * tolerated.
     *
     * <p>This is the recommended entry point for business handlers that already declare a typed
     * {@code Config} record: the call site stays type-safe while the wire format remains
     * registry- and JSON-agnostic.
     *
     * <pre>
     * record AiReviewConfig(String model, double threshold) {}
     *
     * flow.node("aiReview", b -&gt; b
     *     .type(MyNodeType.AI_REVIEW)
     *     .setAll(new AiReviewConfig("gpt-4", 0.85)));
     * </pre>
     *
     * @throws IllegalArgumentException if the POJO cannot be converted to a JSON-object shape
     *     (i.e. Jackson produces something other than a {@code Map}).
     */
    public NodeBuilder setAll(Object pojo) {
        if (pojo == null) {
            return this;
        }
        Map<String, Object> map;
        try {
            map = MAPPER.convertValue(pojo, MAP_OF_OBJECT);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "setAll(Object): value of type "
                            + pojo.getClass().getName()
                            + " is not convertible to Map<String, Object>",
                    e);
        }
        if (map != null) {
            this.props.putAll(map);
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
     * @throws IllegalStateException if {@link #type(String)} (or {@link #type(NodeType)}) was
     *     never called or yielded a blank value.
     */
    NodeConfig buildConfig() {
        if (type == null || type.isBlank()) {
            throw new IllegalStateException("NodeBuilder.type(...) is required");
        }
        return new NodeConfig(type, Map.copyOf(props));
    }
}
