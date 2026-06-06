package org.carl.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Structural validator for {@link WorkflowDefinition}.
 *
 * <p>Two overloads:
 *
 * <ul>
 *   <li>{@link #validate(WorkflowDefinition)} - structure-only checks (nodes/edges/start/end).
 *   <li>{@link #validate(WorkflowDefinition, NodeHandlerRegistry)} - additionally checks that every
 *       node {@code type} resolves to a registered {@link NodeHandler} and that the node's {@code
 *       config} deserialises into the handler's {@link NodeHandler#configType()}.
 * </ul>
 *
 * <p>Errors are returned as messages with stable prefixes so callers can grep / assert. The
 * validator never throws; use {@link ValidationReport#throwIfInvalid()} if you want fail-fast
 * behaviour.
 */
public final class GraphValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GraphValidator() {
        throw new AssertionError("no instances");
    }

    public static ValidationReport validate(WorkflowDefinition definition) {
        return validate(definition, null);
    }

    public static ValidationReport validate(
            WorkflowDefinition definition, NodeHandlerRegistry registry) {
        Objects.requireNonNull(definition, "definition");
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. id and name. The record constructor already enforces non-null/non-blank, so this
        // is technically belt-and-braces; we still include the check so that a caller passing
        // a partially constructed definition via reflection or json edge case gets a clean
        // error message.
        if (definition.id() == null || definition.id().isBlank()) {
            errors.add("workflow id must not be blank");
        }
        if (definition.name() == null || definition.name().isBlank()) {
            errors.add("workflow name must not be blank");
        }

        // 2. nodes >= 1
        if (definition.nodes().isEmpty()) {
            errors.add("workflow must contain at least one node");
            // Stop early if there are no nodes; subsequent checks reference node ids.
            return new ValidationReport(errors, warnings);
        }

        // 3. node ids unique + 4. type non-blank
        Set<String> seenIds = new HashSet<>();
        Set<String> validNodeIds = new HashSet<>();
        for (NodeDefinition node : definition.nodes()) {
            if (!seenIds.add(node.id())) {
                errors.add("duplicate node id: " + node.id());
            } else {
                validNodeIds.add(node.id());
            }
            if (node.type() == null || node.type().isBlank()) {
                errors.add("node " + node.id() + " type must not be blank");
            }
        }

        // 5. edge from/to must reference existing nodes
        // 6. self-loop -> warning (timerTask explicitly allowed)
        for (EdgeDefinition edge : definition.edges()) {
            if (!validNodeIds.contains(edge.from())) {
                errors.add(
                        "edge references unknown 'from' node: " + edge.from() + " -> " + edge.to());
            }
            if (!validNodeIds.contains(edge.to())) {
                errors.add(
                        "edge references unknown 'to' node: " + edge.from() + " -> " + edge.to());
            }
            if (edge.from().equals(edge.to()) && validNodeIds.contains(edge.from())) {
                NodeDefinition node = findNode(definition, edge.from());
                if (node == null || !NodeTypes.TIMER_TASK.equals(node.type())) {
                    warnings.add("self-loop edge on node: " + edge.from());
                }
            }
        }

        // 7. node type must be in registry (if given) + node config deserialises
        if (registry != null) {
            for (NodeDefinition node : definition.nodes()) {
                if (node.type() == null || node.type().isBlank()) {
                    continue;
                }
                Optional<NodeHandler<?>> handler = registry.find(node.type());
                if (handler.isEmpty()) {
                    errors.add(
                            "node "
                                    + node.id()
                                    + " ("
                                    + node.type()
                                    + ") has no handler registered");
                    continue;
                }
                List<String> configErrors = validateNodeConfig(node, registry);
                errors.addAll(configErrors);
            }
        }

        // Build graph for start/end/reachability/cycle checks. Only safe if no duplicate ids
        // and no dangling edges, but WorkflowGraph itself tolerates these (deduped via
        // last-write-wins; dangling edges skipped during traversal). We still guard against
        // a completely empty/broken state.
        WorkflowGraph graph;
        try {
            graph = new WorkflowGraph(definition);
        } catch (RuntimeException e) {
            errors.add("failed to build graph index: " + e.getMessage());
            return new ValidationReport(errors, warnings);
        }

        // 8. start node resolution
        //    - explicit definition.startNodeId() takes precedence; if set it must reference an
        //      existing node. The "zero incoming" constraint is dropped so back-edges to the
        //      start are legal.
        //    - otherwise we fall back to topology: at least one node with no incoming edges must
        //      exist. Multiple potential starts -> warning (the runtime will refuse to pick
        //      without an explicit hint). Zero potential starts -> error.
        String explicitStart = definition.startNodeId();
        Set<String> topologicalStarts = graph.startNodes();
        Set<String> reachabilityRoots;
        if (explicitStart != null) {
            if (!validNodeIds.contains(explicitStart)) {
                errors.add("explicit startNodeId '" + explicitStart + "' is not a registered node");
                reachabilityRoots = topologicalStarts;
            } else {
                reachabilityRoots = Set.of(explicitStart);
            }
        } else {
            if (topologicalStarts.isEmpty()) {
                errors.add(
                        "workflow must contain at least one start node (no node with zero incoming, and no explicit startNodeId)");
            } else if (topologicalStarts.size() > 1) {
                warnings.add(
                        "multiple potential start nodes: "
                                + topologicalStarts
                                + "; consider setting an explicit startNodeId");
            }
            reachabilityRoots = topologicalStarts;
        }
        if (definition.edges().isEmpty() && definition.nodes().size() > 1) {
            // Multiple nodes but zero edges -> every node is a "start" and an "end" and
            // is isolated. Single-node workflows are legitimate (handler-only flows).
            errors.add("workflow has multiple isolated nodes (no edges); flow is unreachable");
        }

        // 9. at least one end node
        if (graph.endNodes().isEmpty()) {
            errors.add(
                    "workflow must contain at least one end node (no outgoing edges or type=endTask)");
        }

        // 10. unreachable nodes -> warning. Use the explicit start as the sole root when set;
        // otherwise the union of all topology-derived starts.
        if (!reachabilityRoots.isEmpty()) {
            Set<String> reachable = new HashSet<>();
            for (String start : reachabilityRoots) {
                reachable.addAll(graph.reachableFrom(start));
            }
            for (String id : graph.nodeIds()) {
                if (!reachable.contains(id)) {
                    warnings.add("unreachable node: " + id);
                }
            }
        }

        // 11. cycles -> warning
        for (List<String> cycle : graph.detectCycles()) {
            warnings.add("cycle detected: " + String.join(" -> ", cycle));
        }

        return new ValidationReport(errors, warnings);
    }

    /**
     * Try to deserialise {@code node.config()} into the handler's {@link NodeHandler#configType()}.
     *
     * <p>Returns an empty list on success, or a single error message describing the failure.
     * Handlers that declare {@link Void} or {@code null} as their config type are skipped.
     */
    public static List<String> validateNodeConfig(
            NodeDefinition node, NodeHandlerRegistry registry) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(registry, "registry");

        Optional<NodeHandler<?>> handlerOpt = registry.find(node.type());
        if (handlerOpt.isEmpty()) {
            return List.of(
                    "node " + node.id() + " (" + node.type() + ") has no handler registered");
        }
        NodeHandler<?> handler = handlerOpt.get();
        Class<?> configType = handler.configType();
        if (configType == null || configType == Void.class) {
            return List.of();
        }
        JsonNode configNode = node.config();
        if (configNode == null || configNode.isNull() || configNode.isMissingNode()) {
            // A handler that asks for a real config type, but the definition gives no config,
            // should not silently pass. We still attempt the conversion below to let Jackson
            // produce its own diagnostic (some types accept the empty input).
            configNode = MAPPER.createObjectNode();
        }
        try {
            MAPPER.treeToValue(configNode, configType);
            return List.of();
        } catch (Exception e) {
            return List.of(
                    "node "
                            + node.id()
                            + " ("
                            + node.type()
                            + ") config invalid: "
                            + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg == null ? cur.getClass().getSimpleName() : msg;
    }

    private static NodeDefinition findNode(WorkflowDefinition definition, String id) {
        for (NodeDefinition n : definition.nodes()) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }
}
