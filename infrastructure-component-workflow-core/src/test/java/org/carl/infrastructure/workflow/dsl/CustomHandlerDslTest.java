package org.carl.infrastructure.workflow.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the zero-intrusion extensibility contract: a business-defined node type can be
 * registered in the DSL without modifying any {@code workflow-core} code. This test deliberately
 * uses only DSL primitives and the {@link WorkflowDefinition} record — no custom-handler import, no
 * handler registry — to prove the DSL layer does not require any cooperating code in {@code
 * workflow-core} to accept a new type.
 */
class CustomHandlerDslTest {

    @Test
    void customNodeTypePropagatesThroughDslBuild() {
        FlowDef flow = Flow.define("aiFlow", "AI 流程");
        flow.start("triage");
        flow.node("triage", b -> b.type("aiReview").set("model", "gpt-4").set("threshold", 0.85));
        flow.node("done", b -> b.type("endTask"));
        flow.from("triage").on("done").to("done");

        WorkflowDefinition def = flow.build();

        NodeDefinition triage =
                def.nodes().stream().filter(n -> "triage".equals(n.id())).findFirst().orElseThrow();

        assertEquals("aiReview", triage.type());

        JsonNode config = triage.config();
        assertNotNull(config);
        assertTrue(config.isObject());
        assertEquals("gpt-4", config.path("model").asText());
        assertEquals(0.85, config.path("threshold").asDouble());
    }

    @Test
    void customNodeTypeWithLabelOverride() {
        FlowDef flow = Flow.define("aiFlow", "AI 流程");
        flow.start("triage");
        flow.node("triage", b -> b.type("aiReview").label("AI 审核").set("model", "claude-opus-4-7"));
        flow.node("done", b -> b.type("endTask"));
        flow.from("triage").on("done").to("done");

        WorkflowDefinition def = flow.build();

        NodeDefinition triage =
                def.nodes().stream().filter(n -> "triage".equals(n.id())).findFirst().orElseThrow();

        assertEquals("aiReview", triage.type());
        assertEquals("AI 审核", triage.label());
        assertEquals("claude-opus-4-7", triage.config().path("model").asText());
    }
}
