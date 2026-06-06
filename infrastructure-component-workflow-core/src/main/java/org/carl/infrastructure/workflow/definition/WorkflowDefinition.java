package org.carl.infrastructure.workflow.definition;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Complete workflow definition. Serializable, storable, and emitted both by the Java DSL and the UI
 * configuration tool.
 *
 * <p>The canonical constructor wraps {@code nodes}/{@code edges} via {@link List#copyOf} to
 * guarantee immutability against external modification of the source lists.
 *
 * <p>{@code startNodeId} is optional. When present it pins the workflow entry point, which is
 * required for graphs containing back-edges to the start (otherwise no node has zero incoming edges
 * and topology-based start detection fails). When {@code null} the runtime falls back to
 * topology-based start detection via {@link
 * org.carl.infrastructure.workflow.graph.WorkflowGraph#startNodes()}.
 *
 * <p>The class is annotated with {@link JsonInclude#NON_NULL} so legacy JSON definitions that omit
 * {@code startNodeId} continue to round-trip unchanged.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowDefinition(
        String id,
        String name,
        List<NodeDefinition> nodes,
        List<EdgeDefinition> edges,
        String startNodeId) {

    public WorkflowDefinition {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        if (startNodeId != null) {
            if (startNodeId.isBlank()) {
                throw new IllegalArgumentException("startNodeId must not be blank");
            }
            boolean found = false;
            for (NodeDefinition n : nodes) {
                if (startNodeId.equals(n.id())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "startNodeId '" + startNodeId + "' is not in nodes");
            }
        }
    }

    /**
     * Backwards-compatible factory: construct a {@link WorkflowDefinition} without an explicit
     * {@code startNodeId}. Equivalent to {@code new WorkflowDefinition(id, name, nodes, edges,
     * null)} but easier to grep for and migrate later.
     */
    public static WorkflowDefinition of(
            String id, String name, List<NodeDefinition> nodes, List<EdgeDefinition> edges) {
        return new WorkflowDefinition(id, name, nodes, edges, null);
    }
}
