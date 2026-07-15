package org.carl.infra.workflow.example.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infra.workflow.definition.NodeStatus;
import org.carl.infra.workflow.definition.WorkflowDefinition;
import org.carl.infra.workflow.dsl.BuiltInNodes;
import org.carl.infra.workflow.dsl.Flow;
import org.carl.infra.workflow.dsl.FlowDef;
import org.carl.infra.workflow.handlers.BuiltInHandlers;
import org.carl.infra.workflow.runtime.BusinessActivityRegistry;
import org.carl.infra.workflow.runtime.EngineConfig;
import org.carl.infra.workflow.runtime.WorkflowEngine;
import org.carl.infra.workflow.runtime.WorkflowHandle;
import org.carl.infra.workflow.runtime.WorkflowResult;
import org.carl.infra.workflow.spi.NodeHandlerRegistry;
import org.carl.infra.workflow.spi.NodeTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

/**
 * Proves the {@link WorkflowEngine} facade runs an approval workflow end-to-end with <strong>zero
 * {@code io.temporal.*} imports</strong> in this (business-side) file — Temporal is fully
 * encapsulated by the facade. Runs against a remote Temporal only when {@code TEMPORAL_TARGET} is
 * set.
 */
@EnabledIfEnvironmentVariable(named = "TEMPORAL_TARGET", matches = ".+")
class WorkflowEngineRemoteTest {

    @Test
    @Timeout(60)
    void facadeRunsApprovalFlow() throws Exception {
        NodeHandlerRegistry handlers = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(handlers);

        BusinessActivityRegistry activities = new BusinessActivityRegistry();
        activities.register("createLeave", in -> Map.<String, Object>of("requestId", "REQ-1"));
        activities.register("notify", in -> Map.<String, Object>of("notified", Boolean.TRUE));

        try (WorkflowEngine engine =
                WorkflowEngine.connect(
                                EngineConfig.of(
                                        System.getenv("TEMPORAL_TARGET"), "ENGINE_FACADE_TEST"))
                        .withWorker(handlers, activities)) {

            WorkflowHandle handle = engine.start(approvalFlow(), Map.of("employee", "alice"));

            Thread.sleep(2000); // let the workflow reach the approval node
            handle.signal("approval", decision("approved"));

            WorkflowResult result = handle.awaitResult();
            assertEquals(NodeStatus.COMPLETED, result.finalStatus());
            assertEquals("completed", result.finalNodeId());
        }
    }

    private static ObjectNode decision(String value) {
        return JsonNodeFactory.instance.objectNode().put("decision", value);
    }

    private static WorkflowDefinition approvalFlow() {
        FlowDef flow = Flow.define("engineFacade", "门面审批");
        flow.start("requestLeave");
        flow.node("requestLeave", BuiltInNodes.service("createLeave"));
        flow.node("approval", BuiltInNodes.approval("mgr"));
        flow.node("notify", BuiltInNodes.service("notify"));
        flow.node("completed", b -> b.type(NodeTypes.END_TASK).label("完成"));
        flow.node("rejected", b -> b.type(NodeTypes.END_TASK).label("拒绝"));
        flow.from("requestLeave").on("SUCCESS").to("approval");
        flow.from("approval").on("APPROVED").to("notify");
        flow.from("approval").on("REJECTED").to("rejected");
        flow.from("notify").on("SUCCESS").to("completed");
        return flow.build();
    }
}
