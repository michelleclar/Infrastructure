package org.carl.infrastructure.workflow.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

class NodeBuilderTest {

    @Test
    void chainedTypeAndSetProducesNodeConfig() {
        NodeBuilder b = new NodeBuilder();
        b.type("aiReview").set("model", "gpt-4").set("temperature", 0.2);

        NodeConfig cfg = b.buildConfig();

        assertEquals("aiReview", cfg.type());
        assertEquals("gpt-4", cfg.props().get("model"));
        assertEquals(0.2, cfg.props().get("temperature"));
    }

    @Test
    void buildConfigWithoutTypeThrows() {
        NodeBuilder b = new NodeBuilder();
        b.set("foo", "bar");
        IllegalStateException ex = assertThrows(IllegalStateException.class, b::buildConfig);
        assertTrue(ex.getMessage().contains("type"));
    }

    @Test
    void buildConfigWithBlankTypeThrows() {
        NodeBuilder b = new NodeBuilder().type("   ");
        assertThrows(IllegalStateException.class, b::buildConfig);
    }

    @Test
    void setAllBulkWritesAllEntries() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", "two");

        NodeConfig cfg = new NodeBuilder().type("custom").setAll(input).buildConfig();

        assertEquals(1, cfg.props().get("a"));
        assertEquals("two", cfg.props().get("b"));
    }

    @Test
    void setAllNullInputIsNoOp() {
        NodeConfig cfg = new NodeBuilder().type("custom").setAll((Object) null).set("k", "v").buildConfig();
        assertEquals(Map.of("k", "v"), cfg.props());
    }

    @Test
    void setBeforeSetAllDoesNotConflict() {
        // set(...) writes; subsequent setAll(...) only adds entries from the map. Keys not
        // mentioned by setAll(...) survive intact.
        NodeConfig cfg =
                new NodeBuilder()
                        .type("custom")
                        .set("keep", "yes")
                        .set("override", "before")
                        .setAll(Map.of("override", "after", "extra", "added"))
                        .buildConfig();

        assertEquals("yes", cfg.props().get("keep"));
        assertEquals("after", cfg.props().get("override"));
        assertEquals("added", cfg.props().get("extra"));
    }

    @Test
    void labelPassesThroughToNodeDefinition() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("n1");
        flow.node("n1", b -> b.type("custom").label("Display Name").set("k", "v"));

        WorkflowDefinition def = flow.build();
        assertEquals(1, def.nodes().size());
        NodeDefinition n1 = def.nodes().get(0);
        assertEquals("n1", n1.id());
        assertEquals("Display Name", n1.label());
        assertEquals("custom", n1.type());
        JsonNode config = n1.config();
        assertEquals("v", config.path("k").asText());
    }

    @Test
    void absentLabelFallsBackToId() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("n1");
        flow.node("n1", b -> b.type("custom").set("k", "v"));

        WorkflowDefinition def = flow.build();
        NodeDefinition n1 = def.nodes().get(0);
        assertEquals("n1", n1.label());
        assertEquals(new ObjectMapper().createObjectNode().put("k", "v"), n1.config());
        // sanity: templateId untouched
        assertNull(n1.templateId());
    }
}
