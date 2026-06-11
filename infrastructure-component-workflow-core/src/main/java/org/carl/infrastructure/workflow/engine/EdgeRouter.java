package org.carl.infrastructure.workflow.engine;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.spi.ConditionEvaluator;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;

import java.util.Set;

/**
 * Platform-independent routing decisions: which node starts the workflow, and which outgoing edge
 * to follow after a node completes.
 *
 * <p>Extracted from the Temporal adapter ({@code GenericWorkflowImpl}) so the orchestration's
 * routing logic can be unit-tested without a Temporal runtime. These are pure functions over the
 * {@link WorkflowGraph}, the node outcome and the {@link NodeExecutionContext}; they perform no
 * I/O and call no Temporal API.
 */
public final class EdgeRouter {

    private EdgeRouter() {
        throw new AssertionError("no instances");
    }

    /**
     * Resolve the entry node, in order of precedence:
     *
     * <ol>
     *   <li>{@code requested} (per-invocation override).
     *   <li>{@link WorkflowDefinition#startNodeId()} (workflow-level explicit start).
     *   <li>The unique zero-incoming node returned by {@link WorkflowGraph#startNodes()}.
     *   <li>Otherwise throws {@link IllegalStateException}.
     * </ol>
     */
    public static String resolveStartNode(
            String requested, WorkflowDefinition definition, WorkflowGraph graph) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        if (definition.startNodeId() != null && !definition.startNodeId().isBlank()) {
            return definition.startNodeId();
        }
        Set<String> starts = graph.startNodes();
        if (starts.size() != 1) {
            throw new IllegalStateException(
                    "Workflow has " + starts.size() + " start nodes; specify startNodeId");
        }
        return starts.iterator().next();
    }

    /**
     * Pick the edge to follow after a node completes. Matching strategy:
     *
     * <ol>
     *   <li>Outgoing edges whose {@link EdgeDefinition#event()} equals {@code outcome} are
     *       evaluated first — this is the post-G2 path where the DSL {@code .on(name)} writes the
     *       node's outcome into the {@code event} field. The first edge whose {@link
     *       EdgeDefinition#when()} guard expression evaluates to {@code true} (or is {@code
     *       null}/blank) wins.
     *   <li>Legacy fallback: edges whose {@link EdgeDefinition#outcome()} equals {@code outcome}
     *       are evaluated next (for hand-constructed definitions still using the {@code outcome}
     *       field directly).
     *   <li>Otherwise the first outgoing edge with no event/outcome/when wins.
     *   <li>If none match, returns {@code null} → the workflow terminates with the current node as
     *       the final node.
     * </ol>
     *
     * <p>Implemented as a single pass over the current node's outgoing edges (fetched in O(1) from
     * the graph's per-node index): a byEvent edge returns immediately on its first {@code when}-true
     * match (highest priority, order-preserving), while the first matching byOutcome edge and the
     * first unconditional default edge are recorded and used only as fallbacks.
     */
    public static EdgeDefinition pickNextEdge(
            WorkflowGraph graph, String currentNodeId, String outcome, NodeExecutionContext ctx) {
        EdgeDefinition legacyMatch = null; // first when-true byOutcome edge (legacy fallback)
        EdgeDefinition defaultEdge = null; // first edge with no event/outcome/when
        for (EdgeDefinition e : graph.outgoing(currentNodeId)) {
            // 1. byEvent (post-G2 primary): highest priority, first when-true wins.
            if (outcome != null && outcome.equals(e.event()) && matchesCondition(e.when(), ctx)) {
                return e;
            }
            // 2. byOutcome (legacy hand-constructed definitions): remember the first when-true edge.
            if (legacyMatch == null
                    && outcome != null
                    && outcome.equals(e.outcome())
                    && matchesCondition(e.when(), ctx)) {
                legacyMatch = e;
            }
            // 3. default: remember the first unconditional edge.
            if (defaultEdge == null
                    && e.event() == null
                    && e.outcome() == null
                    && e.when() == null) {
                defaultEdge = e;
            }
        }
        return legacyMatch != null ? legacyMatch : defaultEdge;
    }

    /**
     * Evaluate an edge guard. A {@code null}/blank expression matches; an expression that throws is
     * treated as non-matching — the same lenient policy {@link ConditionEvaluator} applies
     * internally (evaluation errors are swallowed rather than aborting routing).
     */
    private static boolean matchesCondition(String expression, NodeExecutionContext ctx) {
        try {
            return ConditionEvaluator.evaluateOrTrue(expression, ctx);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
