package org.carl.infrastructure.workflow.graph;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only query view over a {@link WorkflowDefinition}.
 *
 * <p>Built once over an immutable {@link WorkflowDefinition}, internally indexes nodes and edges
 * for O(1) lookup and provides reachability/cycle analysis.
 *
 * <p>The graph layer never evaluates {@link EdgeDefinition#when()} because it holds no execution
 * context; callers must filter conditional edges with their own runtime state.
 */
public final class WorkflowGraph {

    private final WorkflowDefinition definition;
    private final Map<String, NodeDefinition> nodesById;
    private final Map<String, List<EdgeDefinition>> outgoingByNodeId;
    private final Map<String, List<EdgeDefinition>> incomingByNodeId;

    public WorkflowGraph(WorkflowDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");

        Map<String, NodeDefinition> nodes = new LinkedHashMap<>();
        for (NodeDefinition node : definition.nodes()) {
            // Duplicate ids are checked by GraphValidator. Last-write-wins here so we never
            // fail construction; querying still works deterministically against the latest.
            nodes.put(node.id(), node);
        }
        this.nodesById = Collections.unmodifiableMap(nodes);

        Map<String, List<EdgeDefinition>> outgoing = new HashMap<>();
        Map<String, List<EdgeDefinition>> incoming = new HashMap<>();
        for (String id : nodes.keySet()) {
            outgoing.put(id, new ArrayList<>());
            incoming.put(id, new ArrayList<>());
        }
        for (EdgeDefinition edge : definition.edges()) {
            outgoing.computeIfAbsent(edge.from(), k -> new ArrayList<>()).add(edge);
            incoming.computeIfAbsent(edge.to(), k -> new ArrayList<>()).add(edge);
        }
        // Freeze the inner lists so callers cannot mutate them.
        Map<String, List<EdgeDefinition>> outgoingFrozen = new HashMap<>(outgoing.size());
        for (Map.Entry<String, List<EdgeDefinition>> e : outgoing.entrySet()) {
            outgoingFrozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        Map<String, List<EdgeDefinition>> incomingFrozen = new HashMap<>(incoming.size());
        for (Map.Entry<String, List<EdgeDefinition>> e : incoming.entrySet()) {
            incomingFrozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        this.outgoingByNodeId = Collections.unmodifiableMap(outgoingFrozen);
        this.incomingByNodeId = Collections.unmodifiableMap(incomingFrozen);
    }

    public WorkflowDefinition definition() {
        return definition;
    }

    // ----- node queries -----

    public NodeDefinition node(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        NodeDefinition node = nodesById.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }
        return node;
    }

    public Optional<NodeDefinition> findNode(String nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    public Set<String> nodeIds() {
        // LinkedHashMap.keySet preserves insertion order; expose it as an unmodifiable set.
        return Collections.unmodifiableSet(nodesById.keySet());
    }

    // ----- edge queries -----

    public List<EdgeDefinition> outgoing(String nodeId) {
        requireKnownNode(nodeId);
        return outgoingByNodeId.getOrDefault(nodeId, List.of());
    }

    public List<EdgeDefinition> incoming(String nodeId) {
        requireKnownNode(nodeId);
        return incomingByNodeId.getOrDefault(nodeId, List.of());
    }

    // ----- routing -----

    public List<EdgeDefinition> nextCandidates(String currentNodeId, EdgeMatch match) {
        Objects.requireNonNull(match, "match");
        List<EdgeDefinition> all = outgoing(currentNodeId);
        if (match instanceof EdgeMatch.Any) {
            return all;
        }
        if (match instanceof EdgeMatch.ByEvent be) {
            List<EdgeDefinition> picked = new ArrayList<>();
            for (EdgeDefinition edge : all) {
                if (be.eventName().equals(edge.event())) {
                    picked.add(edge);
                }
            }
            return List.copyOf(picked);
        }
        if (match instanceof EdgeMatch.ByOutcome bo) {
            List<EdgeDefinition> picked = new ArrayList<>();
            for (EdgeDefinition edge : all) {
                if (bo.outcome().equals(edge.outcome())) {
                    picked.add(edge);
                }
            }
            return List.copyOf(picked);
        }
        // Sealed; defensive fall-through never reached.
        throw new IllegalStateException("Unsupported EdgeMatch: " + match);
    }

    public boolean canAccept(String currentNodeId, WorkflowEvent event) {
        Objects.requireNonNull(event, "event");
        return !nextCandidates(currentNodeId, EdgeMatch.byEvent(event.name())).isEmpty();
    }

    // ----- reachability -----

    public boolean canReach(String fromNodeId, String toNodeId) {
        requireKnownNode(fromNodeId);
        requireKnownNode(toNodeId);
        if (fromNodeId.equals(toNodeId)) {
            return true;
        }
        return reachableFrom(fromNodeId).contains(toNodeId);
    }

    public Set<String> reachableFrom(String nodeId) {
        requireKnownNode(nodeId);
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(nodeId);
        visited.add(nodeId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (EdgeDefinition edge : outgoingByNodeId.getOrDefault(current, List.of())) {
                String target = edge.to();
                if (!nodesById.containsKey(target)) {
                    // Dangling edges (from validator's POV) are skipped during traversal.
                    continue;
                }
                if (visited.add(target)) {
                    queue.add(target);
                }
            }
        }
        return Collections.unmodifiableSet(visited);
    }

    // ----- cycle detection (Tarjan's SCC) -----

    /**
     * Find every cycle (strongly-connected component) in the graph.
     *
     * <p>Returns a list of SCCs where each SCC has either &gt;= 2 nodes or is a single node with a
     * self-loop. SCCs of a single node without a self-loop are excluded because they are not
     * cycles.
     *
     * <p>Node ordering inside each SCC follows iteration order of {@link #nodeIds()} for stable
     * test assertions.
     */
    public List<List<String>> detectCycles() {
        TarjanState state = new TarjanState();
        for (String id : nodesById.keySet()) {
            if (!state.indexOf.containsKey(id)) {
                strongConnect(id, state);
            }
        }
        // Keep only non-trivial SCCs (size > 1, or single node with self-loop).
        List<List<String>> result = new ArrayList<>();
        for (List<String> scc : state.sccs) {
            if (scc.size() > 1) {
                result.add(stableOrder(scc));
            } else {
                String only = scc.get(0);
                if (hasSelfLoop(only)) {
                    result.add(List.of(only));
                }
            }
        }
        return List.copyOf(result);
    }

    private List<String> stableOrder(List<String> scc) {
        Set<String> set = new HashSet<>(scc);
        List<String> ordered = new ArrayList<>(scc.size());
        for (String id : nodesById.keySet()) {
            if (set.contains(id)) {
                ordered.add(id);
            }
        }
        return List.copyOf(ordered);
    }

    private boolean hasSelfLoop(String nodeId) {
        for (EdgeDefinition edge : outgoingByNodeId.getOrDefault(nodeId, List.of())) {
            if (nodeId.equals(edge.to())) {
                return true;
            }
        }
        return false;
    }

    private void strongConnect(String v, TarjanState state) {
        // Iterative Tarjan to avoid blowing the JVM stack on deep graphs.
        Deque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(v, iteratorOf(v)));
        state.indexOf.put(v, state.index);
        state.lowlink.put(v, state.index);
        state.index++;
        state.tarjanStack.push(v);
        state.onStack.add(v);

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (frame.iter.hasNext()) {
                String w = frame.iter.next();
                if (!nodesById.containsKey(w)) {
                    continue;
                }
                if (!state.indexOf.containsKey(w)) {
                    state.indexOf.put(w, state.index);
                    state.lowlink.put(w, state.index);
                    state.index++;
                    state.tarjanStack.push(w);
                    state.onStack.add(w);
                    stack.push(new Frame(w, iteratorOf(w)));
                } else if (state.onStack.contains(w)) {
                    state.lowlink.put(
                            frame.nodeId,
                            Math.min(state.lowlink.get(frame.nodeId), state.indexOf.get(w)));
                }
            } else {
                stack.pop();
                if (state.lowlink.get(frame.nodeId).equals(state.indexOf.get(frame.nodeId))) {
                    List<String> scc = new ArrayList<>();
                    String w;
                    do {
                        w = state.tarjanStack.pop();
                        state.onStack.remove(w);
                        scc.add(w);
                    } while (!w.equals(frame.nodeId));
                    state.sccs.add(scc);
                }
                if (!stack.isEmpty()) {
                    String parent = stack.peek().nodeId;
                    state.lowlink.put(
                            parent,
                            Math.min(state.lowlink.get(parent), state.lowlink.get(frame.nodeId)));
                }
            }
        }
    }

    private java.util.Iterator<String> iteratorOf(String nodeId) {
        List<EdgeDefinition> edges = outgoingByNodeId.getOrDefault(nodeId, List.of());
        List<String> targets = new ArrayList<>(edges.size());
        for (EdgeDefinition edge : edges) {
            targets.add(edge.to());
        }
        return targets.iterator();
    }

    private static final class Frame {
        final String nodeId;
        final java.util.Iterator<String> iter;

        Frame(String nodeId, java.util.Iterator<String> iter) {
            this.nodeId = nodeId;
            this.iter = iter;
        }
    }

    private static final class TarjanState {
        int index = 0;
        final Map<String, Integer> indexOf = new HashMap<>();
        final Map<String, Integer> lowlink = new HashMap<>();
        final Deque<String> tarjanStack = new ArrayDeque<>();
        final Set<String> onStack = new HashSet<>();
        final List<List<String>> sccs = new ArrayList<>();
    }

    // ----- start / end -----

    public Set<String> startNodes() {
        Set<String> starts = new LinkedHashSet<>();
        for (String id : nodesById.keySet()) {
            if (hasNoExternalIncoming(id)) {
                starts.add(id);
            }
        }
        return Collections.unmodifiableSet(starts);
    }

    private boolean hasNoExternalIncoming(String id) {
        for (EdgeDefinition edge : incomingByNodeId.getOrDefault(id, List.of())) {
            // Self-loops do not count as 'external' incoming — they cannot bring control
            // flow into the node from outside, so a node whose only incoming edges are
            // self-loops is still a valid start node.
            if (!edge.from().equals(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resolve the entry node the runtime should start at.
     *
     * <ol>
     *   <li>If {@link WorkflowDefinition#startNodeId()} is set, return it (existence is guaranteed
     *       by the record canonical constructor).
     *   <li>Otherwise, if topology yields exactly one node with zero incoming edges, return that
     *       node.
     *   <li>Otherwise throw {@link IllegalStateException} — callers must disambiguate via an
     *       explicit startNodeId.
     * </ol>
     */
    public String effectiveStartNode() {
        String explicit = definition.startNodeId();
        if (explicit != null) {
            return explicit;
        }
        Set<String> starts = startNodes();
        if (starts.size() == 1) {
            return starts.iterator().next();
        }
        throw new IllegalStateException(
                "Cannot resolve start node: "
                        + starts.size()
                        + " topological start node(s); set startNodeId");
    }

    public Set<String> endNodes() {
        Set<String> ends = new LinkedHashSet<>();
        for (Map.Entry<String, NodeDefinition> entry : nodesById.entrySet()) {
            String id = entry.getKey();
            boolean noOutgoing = outgoingByNodeId.getOrDefault(id, List.of()).isEmpty();
            boolean isEndType = NodeTypes.END_TASK.equals(entry.getValue().type());
            if (noOutgoing || isEndType) {
                ends.add(id);
            }
        }
        return Collections.unmodifiableSet(ends);
    }

    // ----- internals -----

    private void requireKnownNode(String nodeId) {
        Objects.requireNonNull(nodeId, "nodeId");
        if (!nodesById.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }
    }
}
