package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal in-memory {@link NodeExecutionContext} used by handler unit tests.
 *
 * <p>Mutable on purpose: tests can pre-seed historical results and variables, then exercise the
 * handler under test without mocking the runtime.
 */
final class TestContext implements NodeExecutionContext {

    private final Map<String, NodeResult> results = new HashMap<>();
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private String workflowId = "wf";
    private String instanceId = "inst";
    private String currentNodeId = "node";
    private JsonNode businessData;
    private WorkflowEvent currentEvent;

    TestContext setResult(String nodeId, NodeResult result) {
        results.put(nodeId, result);
        return this;
    }

    TestContext setVariable(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    TestContext setCurrentNodeId(String id) {
        this.currentNodeId = id;
        return this;
    }

    TestContext setCurrentEvent(WorkflowEvent event) {
        this.currentEvent = event;
        return this;
    }

    TestContext setBusinessData(JsonNode data) {
        this.businessData = data;
        return this;
    }

    TestContext setWorkflowId(String id) {
        this.workflowId = id;
        return this;
    }

    TestContext setInstanceId(String id) {
        this.instanceId = id;
        return this;
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
    public String currentNodeId() {
        return currentNodeId;
    }

    @Override
    public JsonNode businessData() {
        return businessData;
    }

    @Override
    public Map<String, Object> variables() {
        return variables;
    }

    @Override
    public NodeResult resultOf(String nodeId) {
        return results.get(nodeId);
    }

    @Override
    public WorkflowEvent currentEvent() {
        return currentEvent;
    }
}
