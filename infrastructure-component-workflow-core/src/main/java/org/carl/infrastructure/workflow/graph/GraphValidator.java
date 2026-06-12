package org.carl.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.engine.NodeConfigCodec;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        // a partially-constructed or deserialized definition that bypassed the canonical
        // constructor gets a clean, actionable error instead of a later NPE.
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
                // timerTask uses a self-loop as its canonical "wait for fire" pattern — it is
                // not a design error. All other node types having a self-loop are unusual enough
                // to deserve a warning, but we don't block deployment because back-edges on
                // retry/escalation flows are sometimes intentional.
                if (node == null || !NodeTypes.TIMER_TASK.equals(node.type())) {
                    warnings.add("self-loop edge on node: " + edge.from());
                }
            }
        }

        // 7. node type must be in registry (if given) + node config deserialises
        Map<String, NodeHandler<?, ?, ?>> handlersByNodeId = new HashMap<>();
        if (registry != null) {
            for (NodeDefinition node : definition.nodes()) {
                if (node.type() == null || node.type().isBlank()) {
                    continue;
                }
                Optional<NodeHandler<?, ?, ?>> handler = registry.find(node.type());
                if (handler.isEmpty()) {
                    errors.add(
                            "node "
                                    + node.id()
                                    + " ("
                                    + node.type()
                                    + ") has no handler registered");
                    continue;
                }
                // Reuse the handler we just looked up instead of re-querying the registry.
                errors.addAll(validateConfigBinding(node, handler.get()));
                handlersByNodeId.put(node.id(), handler.get());
            }
            for (EdgeDefinition edge : definition.edges()) {
                NodeHandler<?, ?, ?> handler = handlersByNodeId.get(edge.from());
                if (handler == null || edge.event() == null || edge.event().isBlank()) {
                    continue;
                }
                Set<String> outcomes = handler.outcomes();
                if (outcomes == null || outcomes.isEmpty()) {
                    continue;
                }
                if (!outcomes.contains(edge.event())) {
                    errors.add(
                            "edge "
                                    + edge.from()
                                    + " -> "
                                    + edge.to()
                                    + " event '"
                                    + edge.event()
                                    + "' is not declared by handler outcomes for node "
                                    + edge.from()
                                    + " ("
                                    + handler.type()
                                    + ")");
                }
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
        //      start (e.g. rejection loops that re-enter the first step) are legal.
        //    - otherwise we fall back to topology: at least one node with no incoming edges must
        //      exist. Multiple potential starts -> warning (the runtime will refuse to pick
        //      without an explicit hint). Zero potential starts -> error (all-cycle graph).
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
            // is isolated. Single-node workflows are legitimate (handler-only flows). With
            // more than one node and no edges, control flow can never leave the start node,
            // making all other nodes permanently unreachable.
            errors.add("workflow has multiple isolated nodes (no edges); flow is unreachable");
        }

        // 9. at least one end node
        if (graph.endNodes().isEmpty()) {
            errors.add(
                    "workflow must contain at least one end node (no outgoing edges or type=endTask)");
        }

        // 10. unreachable nodes -> warning only (not an error), because unreachable nodes may be
        // intentional dead branches kept for documentation or staged rollout. The runtime will
        // simply never visit them. Use the explicit start as the sole root when set; otherwise
        // take the union of all topology-derived starts as BFS roots.
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

        // 11. cycles -> warning only. Cycles are legal in workflows that implement retry, escalation,
        // or recurring timer patterns. Flagging them helps reviewers spot unintentional back-edges
        // without blocking valid definitions.
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

        Optional<NodeHandler<?, ?, ?>> handlerOpt = registry.find(node.type());
        if (handlerOpt.isEmpty()) {
            return List.of(
                    "node " + node.id() + " (" + node.type() + ") has no handler registered");
        }
        return validateConfigBinding(node, handlerOpt.get());
    }

    /** Config-binding check against an already-resolved handler (no registry lookup). */
    private static List<String> validateConfigBinding(
            NodeDefinition node, NodeHandler<?, ?, ?> handler) {
        try {
            NodeConfigCodec.decode(MAPPER, handler, node.config());
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
