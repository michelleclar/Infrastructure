package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.ExecutionRecord;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime-side {@link NodeExecutionContext}. The workflow body owns the underlying state; handlers
 * only ever see the interface view.
 *
 * <p>Mutators ({@link #setCurrentNodeId(String)}, {@link #setCurrentEvent(WorkflowEvent)}, {@link
 * #recordResult(String, NodeResult)}, {@link #putVariable(String, Object)}) are intentionally not
 * part of the {@link NodeExecutionContext} interface; they are invoked by the workflow loop only.
 *
 * <p>The class is single-threaded by Temporal workflow semantics (workflows run one step at a
 * time), so no synchronization is required.
 */
final class ExecutionContext implements NodeExecutionContext {

    private final String workflowId;
    private final String instanceId;
    private final String definitionId;
    private final JsonNode businessData;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final Map<String, NodeResult> nodeResults = new LinkedHashMap<>();
    private final Map<String, Integer> visitCounts = new LinkedHashMap<>();
    private final List<ExecutionRecord> executionLog = new ArrayList<>();

    private String currentNodeId;
    private WorkflowEvent currentEvent;

    ExecutionContext(
            String workflowId,
            String instanceId,
            String definitionId,
            JsonNode businessData,
            Map<String, Object> initialVariables) {
        this.workflowId = workflowId;
        this.instanceId = instanceId;
        this.definitionId = definitionId;
        this.businessData = businessData;
        if (initialVariables != null) {
            this.variables.putAll(initialVariables);
        }
    }

    @Override
    public String workflowId() {
        return workflowId;
    }

    @Override
    public String instanceId() {
        return instanceId;
    }

    @Override
    public String definitionId() {
        return definitionId;
    }

    @Override
    public String currentNodeId() {
        return currentNodeId;
    }

    @Override
    public JsonNode businessData() {
        return businessData;
    }

    @Override
    public Map<String, Object> variables() {
        return Collections.unmodifiableMap(variables);
    }

    @Override
    public NodeResult resultOf(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        return nodeResults.get(nodeId);
    }

    @Override
    public WorkflowEvent currentEvent() {
        return currentEvent;
    }

    // ---- internals used by the workflow loop ----

    void setCurrentNodeId(String nodeId) {
        this.currentNodeId = nodeId;
    }

    void setCurrentEvent(WorkflowEvent event) {
        this.currentEvent = event;
    }

    void recordResult(String qualifier, NodeResult result) {
        if (qualifier == null || result == null) {
            return;
        }
        nodeResults.put(qualifier, result);
        int visitNo =
                (int) executionLog.stream().filter(r -> r.nodeId().equals(qualifier)).count() + 1;
        executionLog.add(new ExecutionRecord(qualifier, visitNo, result));
    }

    @Override
    public List<ExecutionRecord> executionRecords() {
        return Collections.unmodifiableList(executionLog);
    }

    void putVariable(String key, Object value) {
        if (key == null) {
            return;
        }
        variables.put(key, value);
    }

    int nextVisitCount(String nodeId) {
        return visitCounts.merge(nodeId, 1, Integer::sum);
    }

    Map<String, NodeResult> snapshotResults() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(nodeResults));
    }

    Map<String, Object> snapshotVariables() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }
}
