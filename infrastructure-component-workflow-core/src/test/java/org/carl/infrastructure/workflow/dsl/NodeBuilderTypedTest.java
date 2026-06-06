package org.carl.infrastructure.workflow.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.BuiltInNodeType;
import org.carl.infrastructure.workflow.spi.NodeType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Covers the typed overloads added to {@link NodeBuilder}: {@code type(NodeType)} and
 * {@code config(Object pojo)}.
 */
class NodeBuilderTypedTest {

    private record SampleConfig(String activity, Map<String, Object> activityInput) {}

    @Test
    void type_nodeTypeOverload_writesValueString() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node("step1", b -> b.type(BuiltInNodeType.SERVICE_TASK).set("activity", "createOrder"));

        WorkflowDefinition def = flow.build();
        NodeDefinition node = def.nodes().get(0);
        assertEquals("serviceTask", node.type());
    }

    @Test
    void type_nodeTypeOf_factory_wrapsString() {
        NodeType custom = NodeType.of("aiReview");
        assertEquals("aiReview", custom.value());

        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node("custom", b -> b.type(custom).set("model", "gpt-4"));

        WorkflowDefinition def = flow.build();
        assertEquals("aiReview", def.nodes().get(0).type());
    }

    @Test
    void type_nullNodeType_isAccepted_buildFailsWithoutType() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        // type(NodeType) accepts null and stores null; buildConfig() then fails as required.
        assertThrows(
                IllegalStateException.class,
                () -> flow.node("step1", b -> b.type((NodeType) null)));
    }

    @Test
    void type_blankString_failsBuild() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        assertThrows(
                IllegalStateException.class,
                () -> flow.node("step1", b -> b.type("   ")));
    }

    @Test
    void setAll_pojoRecord_isConvertedToProps() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node(
                "step1",
                b ->
                        b.type(BuiltInNodeType.SERVICE_TASK)
                                .setAll(new SampleConfig("createOrder", Map.of("customerId", "alice"))));

        WorkflowDefinition def = flow.build();
        NodeDefinition node = def.nodes().get(0);
        JsonNode config = node.config();
        assertNotNull(config);
        assertEquals("createOrder", config.get("activity").asText());
        assertEquals("alice", config.get("activityInput").get("customerId").asText());
    }

    @Test
    void setAll_pojo_thenSet_mergesWithSetWinning() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node(
                "step1",
                b ->
                        b.type(BuiltInNodeType.SERVICE_TASK)
                                .setAll(new SampleConfig("createOrder", Map.of()))
                                .set("activity", "createOrderV2"));

        WorkflowDefinition def = flow.build();
        // Later set() overrides earlier POJO-derived value.
        assertEquals("createOrderV2", def.nodes().get(0).config().get("activity").asText());
    }

    @Test
    void setAll_nullPojo_isNoop() {
        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node(
                "step1",
                b ->
                        b.type(BuiltInNodeType.SERVICE_TASK)
                                .setAll((Object) null)
                                .set("activity", "createOrder"));

        WorkflowDefinition def = flow.build();
        assertEquals("createOrder", def.nodes().get(0).config().get("activity").asText());
    }

    @Test
    void setAll_mapAndPojo_bothOverloadsWork() {
        // setAll(Map) and setAll(Object pojo) are distinct overloads; both merge into props.
        Map<String, Object> bulk = new LinkedHashMap<>();
        bulk.put("k1", "v1");

        FlowDef flow = Flow.define("typed", "Typed Flow");
        flow.node(
                "step1",
                b ->
                        b.type(BuiltInNodeType.SERVICE_TASK)
                                .setAll(bulk)
                                .setAll(new SampleConfig("createOrder", Map.of())));

        WorkflowDefinition def = flow.build();
        JsonNode config = def.nodes().get(0).config();
        assertEquals("v1", config.get("k1").asText());
        assertEquals("createOrder", config.get("activity").asText());
    }
}
