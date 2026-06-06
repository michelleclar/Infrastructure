package org.carl.infrastructure.workflow.spi;

import java.util.Objects;

/**
 * Typed wrapper around a node-type discriminator. The wire format
 * ({@link org.carl.infrastructure.workflow.definition.NodeDefinition#type()}) stays a plain
 * string for JSON friendliness; this interface exists so the Java DSL layer can express node
 * types with compile-time safety, IDE autocomplete, and refactor support.
 *
 * <p>Three usage patterns:
 *
 * <ul>
 *   <li><strong>Built-ins:</strong> use {@link BuiltInNodeType} enum values
 *       (e.g. {@code BuiltInNodeType.SERVICE_TASK}).</li>
 *   <li><strong>Business-defined types:</strong> declare your own enum implementing
 *       {@code NodeType} co-located with the handler — recommended because it's a single source
 *       of truth and refactor-safe.</li>
 *   <li><strong>Ad-hoc:</strong> {@link #of(String)} wraps an arbitrary string, useful for
 *       JSON-driven flows where the type is loaded at runtime.</li>
 * </ul>
 *
 * <p>The interface is a {@link FunctionalInterface}: anonymous lambdas are fine for one-off
 * cases but business code should prefer the enum form.
 */
@FunctionalInterface
public interface NodeType {

    /** The stable wire-format string used by handlers, JSON, and the registry. */
    String value();

    /**
     * Lightweight ad-hoc wrapper. Useful when the type string is loaded from a JSON definition
     * or a configuration table at runtime.
     *
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is blank
     */
    static NodeType of(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        return () -> value;
    }
}
