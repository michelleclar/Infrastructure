package org.carl.infra.workflow.schedule;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.carl.infra.workflow.definition.WorkflowDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** The workflow execution started each time a schedule fires. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScheduledWorkflowAction(
        String workflowId,
        WorkflowDefinition definition,
        Map<String, Object> businessData,
        Map<String, Object> initialVariables,
        String startNodeId,
        boolean archive) {

    public ScheduledWorkflowAction {
        requireText(workflowId, "workflowId");
        Objects.requireNonNull(definition, "definition");
        businessData = immutableMap(businessData, "businessData");
        initialVariables = immutableMap(initialVariables, "initialVariables");
        if (startNodeId != null) {
            requireText(startNodeId, "startNodeId");
            boolean exists = definition.nodes().stream().anyMatch(node -> startNodeId.equals(node.id()));
            if (!exists) {
                throw new IllegalArgumentException(
                        "startNodeId '" + startNodeId + "' is not in workflow definition");
            }
        }
    }

    /** Creates an action with empty variables, the definition's start node, and archival disabled. */
    public static ScheduledWorkflowAction of(
            String workflowId,
            WorkflowDefinition definition,
            Map<String, Object> businessData) {
        return new ScheduledWorkflowAction(
                workflowId, definition, businessData, Map.of(), null, false);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static Map<String, Object> immutableMap(
            Map<String, Object> values, String field) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        values.forEach(
                (key, value) -> {
                    if (key == null) {
                        throw new IllegalArgumentException(field + " keys must not be null");
                    }
                    copy.put(key, value);
                });
        return Collections.unmodifiableMap(copy);
    }
}
