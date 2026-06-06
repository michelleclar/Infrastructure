package org.carl.infrastructure.workflow.handlers;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.NodeStatus;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Built-in handler for {@code subProcess} nodes. */
public final class SubProcessHandler implements NodeHandler<SubProcessConfig> {

    /** Internal completion event delivered by the runtime when the sub-workflow finishes. */
    public static final String COMPLETED_EVENT = "_subProcessCompleted";

    @Override
    public String type() {
        return NodeTypes.SUB_PROCESS;
    }

    @Override
    public Class<SubProcessConfig> configType() {
        return SubProcessConfig.class;
    }

    @Override
    public Set<String> outcomes() {
        return Set.of(Outcomes.COMPLETED, Outcomes.CANCELLED, Outcomes.FAILED);
    }

    @Override
    public NodeResult run(NodeExecutionContext ctx, SubProcessConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (config != null) {
            if (config.subWorkflowId() != null) {
                payload.put(RuntimeIntents.SUB_WORKFLOW_ID, config.subWorkflowId());
            }
            if (config.definitionJson() != null) {
                payload.put(RuntimeIntents.SUB_DEFINITION_JSON, config.definitionJson());
            }
            if (config.input() != null) {
                payload.put(RuntimeIntents.SUB_INPUT, config.input());
            }
        }
        return new NodeResult(NodeStatus.WAITING, null, payload, null);
    }

    @Override
    public boolean canAccept(NodeExecutionContext ctx, WorkflowEvent ev, SubProcessConfig cfg) {
        return ev != null && COMPLETED_EVENT.equals(ev.name());
    }

    @Override
    public NodeResult onEvent(NodeExecutionContext ctx, WorkflowEvent ev, SubProcessConfig cfg) {
        if (ev == null || !COMPLETED_EVENT.equals(ev.name())) {
            return NodeResult.waiting();
        }
        JsonNode payload = ev.payload();
        String subOutcome = payload == null ? null : payload.path("subOutcome").asText(null);
        if (subOutcome == null || subOutcome.isBlank()) {
            return NodeResult.failed("subProcess completion missing 'subOutcome'");
        }
        String mapped = subOutcome;
        if (cfg != null && cfg.outcomeMapping() != null) {
            String override = cfg.outcomeMapping().get(subOutcome);
            if (override != null && !override.isBlank()) {
                mapped = override;
            }
        }
        return NodeResult.completed(mapped);
    }
}
