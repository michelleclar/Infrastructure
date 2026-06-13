package org.carl.infrastructure.workflow.spi;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeResult;

import java.util.Map;

/**
 * Runtime context exposed to node handlers. Conceptually a <em>read-only view</em> of the running
 * workflow instance: identity, business data, variables, historical node results and the current
 * event (if any).
 *
 * <h2>Determinism</h2>
 *
 * <p>Every method on this interface is required to be deterministic and free of side effects. The
 * runtime guarantees that re-invoking these methods during replay yields the same values that were
 * observed during the original execution. Handlers must not mutate any values reachable from this
 * context directly: changes to workflow state must be expressed via {@link NodeResult#payload()}
 * intents, which the runtime applies outside the deterministic boundary.
 *
 * <p>See {@link NodeHandler} for the full determinism contract.
 */
public interface NodeExecutionContext {

    /**
     * Temporal workflow instance id — unique per running workflow execution (equivalent to {@code
     * WorkflowInfo.getWorkflowId()}). Use {@link #definitionId()} to get the logical workflow type
     * identifier.
     */
    String workflowId();

    /**
     * Temporal run id — unique per workflow attempt (equivalent to {@code
     * WorkflowInfo.getRunId()}). Changes on retry.
     */
    String instanceId();

    /** Definition id of the node currently being evaluated. */
    String currentNodeId();

    /** Immutable business input captured when the workflow instance was started. */
    JsonNode businessData();

    /**
     * Returns an unmodifiable view of the current workflow variables. Handlers must not attempt to
     * mutate this map directly; to change a variable, return a {@link NodeResult} whose {@link
     * NodeResult#payload()} carries a {@code setVariables} map (see {@code
     * RuntimeIntents.SET_VARIABLES}). The runtime applies those entries outside the deterministic
     * boundary, before routing the node's outgoing edge, so guard expressions on outgoing edges
     * observe the updated values.
     */
    Map<String, Object> variables();

    /**
     * Look up the {@link NodeResult} of a previously executed node by id.
     *
     * @return the result, or {@code null} if no such node has executed yet (or it does not exist in
     *     the definition).
     */
    NodeResult resultOf(String nodeId);

    /**
     * The triggering event for the current invocation.
     *
     * @return non-null only during {@link NodeHandler#canAccept(NodeExecutionContext,
     *     WorkflowEvent, Object)} and {@link NodeHandler#onEvent(NodeExecutionContext,
     *     WorkflowEvent, Object)}; otherwise {@code null}.
     */
    WorkflowEvent currentEvent();

    /**
     * Workflow definition identifier — the {@link
     * org.carl.infrastructure.workflow.definition.WorkflowDefinition#id()} value. Stable across all
     * instances of the same workflow type.
     */
    default String definitionId() {
        return null;
    }

    /** Full ordered execution history — all results across all visits, in execution order. */
    default java.util.List<org.carl.infrastructure.workflow.definition.ExecutionRecord>
            executionRecords() {
        return java.util.List.of();
    }
}
