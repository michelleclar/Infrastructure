package org.carl.infrastructure.workflow.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.handlers.ServiceTaskConfig;
import org.carl.infrastructure.workflow.handlers.ServiceTaskHandler;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Pure-Java unit tests for {@link NodeConfigCodec} — <strong>no Temporal runtime involved</strong>.
 * Covers DSL-shape normalization (taskGroup join object → bare string) and typed config decoding,
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
        assertEquals("any", out.nodes().getFirst().config().get("join").asText());
    }

    // ---- decode -------------------------------------------------------------

    @Test
    void decode_bindsServiceTaskConfig() throws Exception {
        JsonNode cfg = MAPPER.readTree("{\"activity\":\"createLeave\"}");
        Object decoded =
                NodeConfigCodec.decode(
                        MAPPER, new ServiceTaskHandler(), cfg);
        assertInstanceOf(ServiceTaskConfig.class, decoded);
        assertEquals("createLeave", ((ServiceTaskConfig) decoded).activity());
    }

    @Test
    void decode_nullConfigYieldsEmptyTypedConfig() {
        // ServiceTaskHandler declares a config type, so null config decodes to an all-null record
        // rather than throwing.
        Object decoded =
                NodeConfigCodec.decode(
                        MAPPER, new ServiceTaskHandler(), null);
        assertInstanceOf(ServiceTaskConfig.class, decoded);
        assertNull(((ServiceTaskConfig) decoded).activity());
    }

    @Test
    void decodeState_bindsBusinessDataToHandlerStateType() throws Exception {
        JsonNode businessData = MAPPER.readTree("{\"owner\":\"alice\",\"amount\":1200}");
        Object decoded = NodeConfigCodec.decodeState(MAPPER, new TypedHandler(), businessData);

        assertInstanceOf(TypedState.class, decoded);
        assertEquals("alice", ((TypedState) decoded).owner());
        assertEquals(1200, ((TypedState) decoded).amount());
    }

    @Test
    void decodeEventPayload_bindsPayloadToHandlerEventType() throws Exception {
        JsonNode payload = MAPPER.readTree("{\"decision\":\"approved\"}");
        Object decoded = NodeConfigCodec.decodeEventPayload(MAPPER, new TypedHandler(), payload);

        assertInstanceOf(TypedEvent.class, decoded);
        assertEquals("approved", ((TypedEvent) decoded).decision());
    }

    private record TypedConfig(String code) {}

    private record TypedState(String owner, int amount) {}

    private record TypedEvent(String decision) {}

    private static final class TypedHandler
            implements NodeHandler<TypedConfig, TypedState, TypedEvent> {

        @Override
        public String type() {
            return "typedCodec";
        }

        @Override
        public Class<TypedConfig> configType() {
            return TypedConfig.class;
        }

        @Override
        public Class<TypedState> stateType() {
            return TypedState.class;
        }

        @Override
        public Class<TypedEvent> eventType() {
            return TypedEvent.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.SUCCESS);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, TypedConfig config) {
            return NodeResult.completed(Outcomes.SUCCESS);
        }
    }
}
