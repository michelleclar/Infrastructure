package org.carl.infrastructure.workflow.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeType;
import org.junit.jupiter.api.Test;

/**
 * End-to-end demonstration that a business-defined {@link NodeType} + typed {@code Config} record
 * round-trips through the DSL → {@link WorkflowDefinition} → JSON serialisation without any
 * code change inside {@code workflow-core}.
 *
 * <p>This is the canonical recipe for adding a custom handler-typed node.
 */
class CustomHandlerTypedDslTest {

    /** Business-side typed node type (lives in the handler's package). */
    enum AiReviewType implements NodeType {
        AI_REVIEW("aiReview");

        private final String value;

        AiReviewType(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }

    /** Business-side typed config record (lives next to the handler). */
    record AiReviewConfig(String model, double threshold) {}

    @Test
    void typedCustomNode_roundTripsThroughDsl() {
        FlowDef flow = Flow.define("aiPipeline", "AI Pipeline");
        flow.node(
                "review",
                b -> b.type(AiReviewType.AI_REVIEW).setAll(new AiReviewConfig("gpt-4", 0.85)));
        flow.node("done", b -> b.type("endTask"));
        flow.from("review").on("APPROVED").to("done");

        WorkflowDefinition def = flow.build();
        NodeDefinition review = def.nodes().get(0);

        assertEquals("aiReview", review.type());
        assertNotNull(review.config());
        assertEquals("gpt-4", review.config().get("model").asText());
        assertEquals(0.85, review.config().get("threshold").asDouble(), 0.0001);
    }

    @Test
    void typedCustomNode_jsonRoundTrip() throws Exception {
        FlowDef flow = Flow.define("aiPipeline", "AI Pipeline");
        flow.node(
                "review",
                b -> b.type(AiReviewType.AI_REVIEW).setAll(new AiReviewConfig("gpt-4", 0.85)));
        flow.from("review").on("APPROVED").to("done");
        flow.node("done", b -> b.type("endTask"));

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(flow.build());
        WorkflowDefinition decoded = mapper.readValue(json, WorkflowDefinition.class);

        NodeDefinition review = decoded.nodes().get(0);
        assertEquals("aiReview", review.type());
        assertEquals("gpt-4", review.config().get("model").asText());

        // Demonstrates that the business can re-derive a typed config from the JSON-stored
        // tree without workflow-core knowing anything about AiReviewConfig.
        AiReviewConfig restored = mapper.treeToValue(review.config(), AiReviewConfig.class);
        assertEquals("gpt-4", restored.model());
        assertEquals(0.85, restored.threshold(), 0.0001);
    }
}
