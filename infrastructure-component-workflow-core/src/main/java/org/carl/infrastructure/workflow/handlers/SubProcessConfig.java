package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Configuration for {@link SubProcessHandler}.
 *
 * @param subWorkflowId identifier / label of the sub-workflow (used in logging and as part of the
 *     child workflow-id).
 * @param definitionJson serialized {@link
 *     org.carl.infrastructure.workflow.definition.WorkflowDefinition} JSON of the child workflow.
 *     Required at runtime; the engine will fail the node if absent.
 * @param input optional structured input passed to the sub-workflow as its {@code businessData}.
 * @param outcomeMapping optional mapping from the child's final outcome to this node's outcome;
 *     when {@code null} or missing a key the child outcome passes through verbatim.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubProcessConfig(
        String subWorkflowId,
        String definitionJson,
        Map<String, Object> input,
        Map<String, String> outcomeMapping) {}
