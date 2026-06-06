package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.carl.infrastructure.workflow.definition.ExecutionRecord;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of a terminated workflow instance sent to the archiver.
 *
 * <p>Carries the minimal data needed for persistence:
 *
 * <ul>
 *   <li>Workflow identity (id, runId, definitionId)
 *   <li>Execution metadata (startedAt, endedAt, status, businessKey)
 *   <li>Terminal state (finalNodeId, finalOutcome, finalStatus)
 *   <li>Payloads (businessDataJson, finalVariables, executionRecords)
 * </ul>
 *
 * @param workflowId Temporal workflowId
 * @param runId Temporal runId
 * @param definitionId {@link org.carl.infrastructure.workflow.definition.WorkflowDefinition#id()}
 * @param businessKey optional business key for correlation
 * @param status COMPLETED/FAILED/CANCELLED
 * @param startedAt when the workflow started
 * @param endedAt when the workflow terminated
 * @param finalNodeId the last node that completed (or failed/cancelled)
 * @param finalOutcome the {@code outcome()} of the terminal node
 * @param finalStatus the {@code status()} of the terminal node
 * @param businessDataJson the immutable business payload from {@link WorkflowInput#businessData()}
 * @param finalVariables snapshot of the mutable variables map at termination
 * @param executionRecords ordered list of every node visit, including repeats via back-edges
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowInstanceSnapshot(
        String workflowId,
        String runId,
        String definitionId,
        String businessKey,
        String status,
        Instant startedAt,
        Instant endedAt,
        String finalNodeId,
        String finalOutcome,
        String finalStatus,
        JsonNode businessDataJson,
        Map<String, Object> finalVariables,
        List<ExecutionRecord> executionRecords) {}
