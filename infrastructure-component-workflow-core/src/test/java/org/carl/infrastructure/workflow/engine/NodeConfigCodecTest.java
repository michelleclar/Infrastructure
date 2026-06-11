package org.carl.infrastructure.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.handlers.ServiceTaskConfig;
import org.carl.infrastructure.workflow.handlers.ServiceTaskHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Pure-Java unit tests for {@link NodeConfigCodec} — <strong>no Temporal runtime involved</strong>.
 * Covers DSL-shape normalisation (taskGroup join object → bare string) and typed config decoding,
 * logic that used to live inside the Temporal adapter.
 */
class NodeConfigCodecTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---- normalizeConfig ----------------------------------------------------

    @Test
    void normalizeConfig_taskGroupJoinObjectFlattenedToString() throws Exception {
        JsonNode raw = MAPPER.readTree("{\"join\":{\"type\":\"all\"},\"tasks\":[]}");
        JsonNode out = NodeConfigCodec.normalizeConfig(NodeTypes.TASK_GROUP, raw);
        assertEquals("all", out.get("join").asText());
    }

    @Test
    void normalizeConfig_nonTaskGroupUnchanged() throws Exception {
        JsonNode raw = MAPPER.readTree("{\"join\":{\"type\":\"all\"}}");
        JsonNode out = NodeConfigCodec.normalizeConfig(NodeTypes.SERVICE_TASK, raw);
        assertSame(raw, out);
    }

    @Test
    void normalizeConfig_nullUnchanged() {
        assertNull(NodeConfigCodec.normalizeConfig(NodeTypes.TASK_GROUP, null));
    }

    // ---- normalizeDefinition ------------------------------------------------

    @Test
    void normalizeDefinition_sameInstanceWhenNothingToNormalize() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(new NodeDefinition("A", null, NodeTypes.SERVICE_TASK, null, null)),
                        List.of());
        assertSame(def, NodeConfigCodec.normalizeDefinition(def));
    }

    @Test
    void normalizeDefinition_flattensTaskGroupNode() throws Exception {
        JsonNode tgConfig = MAPPER.readTree("{\"join\":{\"type\":\"any\"},\"tasks\":[]}");
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w", "W",
                        List.of(new NodeDefinition("tg", null, NodeTypes.TASK_GROUP, null, tgConfig)),
                        List.of());
        WorkflowDefinition out = NodeConfigCodec.normalizeDefinition(def);
        assertNotSame(def, out);
        assertEquals("any", out.nodes().get(0).config().get("join").asText());
    }

    // ---- decode -------------------------------------------------------------

    @Test
    void decode_bindsServiceTaskConfig() throws Exception {
        JsonNode cfg = MAPPER.readTree("{\"activity\":\"createLeave\"}");
        Object decoded =
                NodeConfigCodec.decode(
                        MAPPER, new ServiceTaskHandler(), NodeTypes.SERVICE_TASK, cfg);
        assertTrue(decoded instanceof ServiceTaskConfig);
        assertEquals("createLeave", ((ServiceTaskConfig) decoded).activity());
    }

    @Test
    void decode_nullConfigYieldsEmptyTypedConfig() {
        // ServiceTaskHandler declares a config type, so null config decodes to an all-null record
        // rather than throwing.
        Object decoded =
                NodeConfigCodec.decode(
                        MAPPER, new ServiceTaskHandler(), NodeTypes.SERVICE_TASK, null);
        assertTrue(decoded instanceof ServiceTaskConfig);
        assertNull(((ServiceTaskConfig) decoded).activity());
    }
}
