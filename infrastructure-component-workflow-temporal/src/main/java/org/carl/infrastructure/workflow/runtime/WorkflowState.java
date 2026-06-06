package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.carl.infrastructure.workflow.definition.NodeResult;

import java.util.Map;

/**
 * Snapshot exposed by {@link GenericWorkflow#query()}.
 *
 * @param currentNodeId the node currently executing or being awaited; {@code null} once the
 *     workflow has terminated.
 * @param finished whether the workflow has reached a terminal state.
 * @param nodeResults snapshot of {@link NodeResult}s recorded so far.
 * @param lastEventName the name of the most recently consumed external/internal event, or {@code
 *     null} if none has been delivered yet.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowState(
        String currentNodeId,
        boolean finished,
        Map<String, NodeResult> nodeResults,
        String lastEventName) {}
