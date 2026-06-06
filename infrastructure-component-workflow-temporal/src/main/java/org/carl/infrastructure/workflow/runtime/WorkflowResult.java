package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.carl.infrastructure.workflow.definition.ExecutionRecord;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;

import java.util.List;
import java.util.Map;

/**
 * Result returned by {@link GenericWorkflow#execute(WorkflowInput)}.
 *
 * @param finalNodeId the id of the last node that completed (or failed/cancelled the workflow).
 * @param finalOutcome the {@link NodeResult#outcome()} of that node; {@code null} if there is none.
 * @param finalStatus the {@link NodeResult#status()} of the terminating node.
 * @param nodeResults snapshot of every node that has produced a result, including taskGroup
 *     children stored under {@code parent/child} qualifier. Only the latest result per node id is
 *     retained; use {@link #executionRecords} for the full visit history.
 * @param executionRecords ordered list of every node visit, including repeated visits via
 *     back-edges.
 * @param finalVariables snapshot of the mutable variables map at termination.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowResult(
        String finalNodeId,
        String finalOutcome,
        NodeStatus finalStatus,
        Map<String, NodeResult> nodeResults,
        List<ExecutionRecord> executionRecords,
        Map<String, Object> finalVariables) {}
