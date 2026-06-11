package org.carl.infrastructure.workflow.engine;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.graph.EdgeMatch;
import org.carl.infrastructure.workflow.graph.WorkflowGraph;
import org.carl.infrastructure.workflow.spi.ConditionEvaluator;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;

import java.util.List;
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
     */
    public static EdgeDefinition pickNextEdge(
            WorkflowGraph graph, String currentNodeId, String outcome, NodeExecutionContext ctx) {
        if (outcome != null) {
            // G2: DSL .on(name) writes the outcome name into the event field.
            for (EdgeDefinition candidate :
                    graph.nextCandidates(currentNodeId, EdgeMatch.byEvent(outcome))) {
                if (matchesCondition(candidate.when(), ctx)) {
                    return candidate;
                }
            }
            // Legacy: hand-constructed EdgeDefinition objects may still set the outcome field.
            @SuppressWarnings("deprecation")
            List<EdgeDefinition> legacyOutcomeCandidates =
                    graph.nextCandidates(currentNodeId, EdgeMatch.byOutcome(outcome));
            for (EdgeDefinition candidate : legacyOutcomeCandidates) {
                if (matchesCondition(candidate.when(), ctx)) {
                    return candidate;
                }
            }
        }
        for (EdgeDefinition e : graph.outgoing(currentNodeId)) {
            if (e.event() == null && e.outcome() == null && e.when() == null) {
                return e;
            }
        }
        return null;
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
