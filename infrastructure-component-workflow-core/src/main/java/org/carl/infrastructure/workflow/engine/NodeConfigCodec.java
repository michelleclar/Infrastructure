package org.carl.infrastructure.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform-independent normalisation and decoding of node {@code config} JSON.
 *
 * <p>Extracted from the Temporal adapter ({@code GenericWorkflowImpl}) so config handling can be
 * unit-tested without a Temporal runtime. All methods are pure functions over Jackson trees and
 * the {@link NodeHandler} contract; they perform no I/O and call no Temporal API.
 */
public final class NodeConfigCodec {

    private NodeConfigCodec() {
        throw new AssertionError("no instances");
    }

    /**
     * Return a copy of {@code definition} with every node's {@code config} normalised via {@link
     * #normalizeConfig(String, JsonNode)}. Used once at workflow entry so graph validation sees the
     * same wire shape the per-node decoder will later consume. Returns the original instance
     * unchanged when no node needed normalisation.
     */
    public static WorkflowDefinition normalizeDefinition(WorkflowDefinition definition) {
        List<NodeDefinition> rewritten = new ArrayList<>(definition.nodes().size());
        boolean anyChanged = false;
        for (NodeDefinition node : definition.nodes()) {
            JsonNode normalised = normalizeConfig(node.type(), node.config());
            if (normalised == node.config()) {
                rewritten.add(node);
            } else {
                rewritten.add(
                        new NodeDefinition(
                                node.id(),
                                node.label(),
                                node.type(),
                                node.templateId(),
                                normalised));
                anyChanged = true;
            }
        }
        if (!anyChanged) {
            return definition;
        }
        return new WorkflowDefinition(
                definition.id(),
                definition.name(),
                rewritten,
                definition.edges(),
                definition.startNodeId());
    }

    /**
     * Normalises DSL-emitted shapes into the wire form expected by the handler config records.
     * Currently only {@code taskGroup} needs adjustment: the DSL writes {@code
     * "join":{"type":"all"}} while {@code TaskGroupConfig.JoinRule} deserialises from the bare
     * string {@code "all"}.
     */
    public static JsonNode normalizeConfig(String nodeType, JsonNode rawConfig) {
        if (rawConfig == null || !NodeTypes.TASK_GROUP.equals(nodeType)) {
            return rawConfig;
        }
        if (!rawConfig.isObject()) {
            return rawConfig;
        }
        JsonNode join = rawConfig.get("join");
        if (join == null || !join.isObject()) {
            return rawConfig;
        }
        JsonNode typeNode = join.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            return rawConfig;
        }
        ObjectNode normalized = rawConfig.deepCopy();
        normalized.put("join", typeNode.asText());
        return normalized;
    }

    /**
     * Decode a node's raw {@code config} JSON into the handler's typed config object, normalising
     * DSL shapes first. Returns {@code null} when the handler declares no config type ({@code null}
     * / {@code Void}). Throws {@link IllegalStateException} on a binding failure.
     *
     * @param mapper the Jackson mapper to bind with (passed in rather than held statically so this
     *     stays a pure function and carries no Temporal/runtime dependency)
     */
    public static Object decode(
            ObjectMapper mapper, NodeHandler<?> handler, String nodeType, JsonNode rawConfig) {
        Class<?> configType = handler.configType();
        if (configType == null || configType == Void.class) {
            return null;
        }
        JsonNode normalized = normalizeConfig(nodeType, rawConfig);
        if (normalized == null || normalized.isNull() || normalized.isMissingNode()) {
            normalized = mapper.createObjectNode();
        }
        try {
            return mapper.treeToValue(normalized, configType);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to decode config for node type " + nodeType + ": " + e.getMessage(), e);
        }
    }
}
