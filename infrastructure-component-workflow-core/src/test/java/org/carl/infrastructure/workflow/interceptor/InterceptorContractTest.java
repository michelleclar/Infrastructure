package org.carl.infrastructure.workflow.interceptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.spi.WorkflowEvent;
import org.junit.jupiter.api.Test;

class InterceptorContractTest {

    @Test
    void deterministicDefaultsAreAllNoOp() {
        DeterministicInterceptor det = new DeterministicInterceptor() {};
        InterceptorContext ctx = stubContext();
        NodeDefinition node = stubNode();
        NodeResult result = NodeResult.completed("SUCCESS");
        WorkflowEvent event = new WorkflowEvent("anyEvent", null);

        assertDoesNotThrow(() -> det.onWorkflowStart(ctx));
        assertDoesNotThrow(() -> det.onWorkflowEnd(ctx, result));
        assertDoesNotThrow(() -> det.onNodeEnter(ctx, node));
        assertDoesNotThrow(() -> det.onNodeExit(ctx, node, result));
        assertDoesNotThrow(() -> det.onNodeError(ctx, node, "boom"));
        assertDoesNotThrow(() -> det.onEvent(ctx, event));
        assertDoesNotThrow(() -> det.onCompensate(ctx, node, result));
    }

    @Test
    void asyncDefaultsAreAllNoOp() {
        AsyncInterceptor async = new AsyncInterceptor() {};
        InterceptorContext ctx = stubContext();
        NodeDefinition node = stubNode();
        NodeResult result = NodeResult.completed("SUCCESS");
        WorkflowEvent event = new WorkflowEvent("anyEvent", null);

        assertDoesNotThrow(() -> async.onWorkflowStart(ctx));
        assertDoesNotThrow(() -> async.onWorkflowEnd(ctx, result));
        assertDoesNotThrow(() -> async.onNodeEnter(ctx, node));
        assertDoesNotThrow(() -> async.onNodeExit(ctx, node, result));
        assertDoesNotThrow(() -> async.onNodeError(ctx, node, "boom"));
        assertDoesNotThrow(() -> async.onEvent(ctx, event));
        assertDoesNotThrow(() -> async.onCompensate(ctx, node, result));
    }

    @Test
    void defaultOrderIsZero() {
        DeterministicInterceptor det = new DeterministicInterceptor() {};
        AsyncInterceptor async = new AsyncInterceptor() {};

        assertEquals(0, det.order());
        assertEquals(0, async.order());
    }

    private static InterceptorContext stubContext() {
        return new InterceptorContext() {
            @Override
            public String workflowId() {
                return "wf-1";
            }

            @Override
            public String instanceId() {
                return "run-1";
            }

            @Override
            public String definitionId() {
                return "def-1";
            }

            @Override
            public JsonNode businessData() {
                return null;
            }
        };
    }

    private static NodeDefinition stubNode() {
        return new NodeDefinition("node-1", null, "serviceTask", null, null);
    }
}
