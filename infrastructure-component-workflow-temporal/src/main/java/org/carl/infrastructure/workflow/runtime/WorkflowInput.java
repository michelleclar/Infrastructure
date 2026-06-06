package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.workflow.definition.WorkflowDefinition;

import java.util.Map;

/**
 * Input passed to {@link GenericWorkflow#execute(WorkflowInput)}.
 *
 * <p>All fields are JSON-serializable Java objects; Temporal automatically handles serialization
 * when transmitting the workflow input to the worker.
 *
 * @param workflowDefinition required, the workflow definition.
 * @param businessData optional, business payload available to handlers via {@link
 *     org.carl.infrastructure.workflow.spi.NodeExecutionContext#businessData()}.
 * @param initialVariables optional, seed for the mutable variables map.
 * @param startNodeId optional. When {@code null}/blank, the runtime falls back to the unique start
 *     node returned by {@link org.carl.infrastructure.workflow.graph.WorkflowGraph#startNodes()}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowInput(
        WorkflowDefinition workflowDefinition,
        JsonNode businessData,
        Map<String, Object> initialVariables,
        String startNodeId) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Convenience method to create a WorkflowInput from a WorkflowDefinition.
     *
     * @param definition the workflow definition
     * @param businessData business data as a Map
     * @return a new WorkflowInput instance
     */
    public static WorkflowInput from(
            WorkflowDefinition definition, Map<String, Object> businessData) {
        return from(definition, businessData, Map.of(), null);
    }

    /**
     * Full convenience method to create a WorkflowInput.
     *
     * @param definition the workflow definition
     * @param businessData business data as a Map
     * @param initialVariables initial variables
     * @param startNodeId optional start node ID
     * @return a new WorkflowInput instance
     */
    public static WorkflowInput from(
            WorkflowDefinition definition,
            Map<String, Object> businessData,
            Map<String, Object> initialVariables,
            String startNodeId) {
        // valueToTree(null) returns NullNode, which then fails the historical ObjectNode cast.
        // Normalise null/missing to an empty ObjectNode so downstream consumers always see a
        // mutable object container.
        ObjectNode dataJson;
        if (businessData == null) {
            dataJson = MAPPER.createObjectNode();
        } else {
            JsonNode tree = MAPPER.valueToTree(businessData);
            dataJson = (tree instanceof ObjectNode on) ? on : MAPPER.createObjectNode();
        }
        return new WorkflowInput(definition, dataJson, initialVariables, startNodeId);
    }
}
