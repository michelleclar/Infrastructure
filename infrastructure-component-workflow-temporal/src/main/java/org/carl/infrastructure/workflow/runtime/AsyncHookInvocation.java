package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

/**
 * Serializable payload dispatched from {@link GenericWorkflowImpl} to {@link
 * AsyncInterceptorActivity} for each async hook invocation.
 *
 * <p>All fields are JSON-serializable. Fields that are not applicable to a given phase are
 * {@code null}.
 *
 * @param phase       one of the constants in {@link HookPhases}
 * @param workflowId  Temporal workflow instance id
 * @param instanceId  Temporal run id
 * @param definitionId workflow definition id
 * @param businessData immutable business input captured at workflow start
 * @param node        applicable for NODE_ENTER / NODE_EXIT / NODE_ERROR / COMPENSATE; otherwise null
 * @param nodeResult  applicable for NODE_EXIT / WORKFLOW_END / COMPENSATE; otherwise null
 * @param errorMessage applicable for NODE_ERROR; otherwise null
 * @param event       applicable for EVENT; otherwise null
 */
public record AsyncHookInvocation(
        String phase,
        String workflowId,
        String instanceId,
        String definitionId,
        JsonNode businessData,
        NodeDefinition node,
        NodeResult nodeResult,
        String errorMessage,
        WorkflowEvent event) {}
