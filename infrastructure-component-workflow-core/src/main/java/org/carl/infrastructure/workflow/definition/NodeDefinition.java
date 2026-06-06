package org.carl.infrastructure.workflow.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Definition of a single node inside a {@link WorkflowDefinition}.
 *
 * <p>Field semantics:
 *
 * <ul>
 *   <li>{@code id}: stable internal identifier, required, used by edges/conditions/runtime records.
 *   <li>{@code label}: display name shown in the UI; optional, falls back to {@code id} when
 *       absent.
 *   <li>{@code type}: node type discriminator, required, e.g. {@code serviceTask}, {@code
 *       approvalTask}, {@code taskGroup}.
 *   <li>{@code templateId}: optional reference to a UI node template.
 *   <li>{@code config}: optional structured configuration interpreted by the matching {@code
 *       NodeHandler}.
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NodeDefinition(
        String id, String label, String type, String templateId, JsonNode config) {

    public NodeDefinition {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(type, "type");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        // label, templateId, config remain optional / nullable.
    }
}
