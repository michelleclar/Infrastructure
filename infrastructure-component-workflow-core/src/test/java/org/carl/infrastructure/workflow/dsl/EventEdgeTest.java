package org.carl.infrastructure.workflow.dsl;

import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.approval;
import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.BuiltInNodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Verifies that {@code FlowFrom#on(...)} and {@code FlowJoin#on(...)} write the supplied name into
 * {@link EdgeDefinition#event()} and leave {@link EdgeDefinition#outcome()} {@code null}.
 */
class EventEdgeTest {

    private static EdgeDefinition firstEdgeFrom(WorkflowDefinition def, String from) {
        return def.edges().stream().filter(e -> from.equals(e.from())).findFirst().orElseThrow();
    }

    @Test
    void simpleOnWritesEventNotOutcome() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("X");
        flow.node("X", service("doX"));
        flow.node("Y", b -> b.type(BuiltInNodeType.END_TASK));
        flow.from("X").on("提交").to("Y");

        WorkflowDefinition def = flow.build();
        EdgeDefinition edge = firstEdgeFrom(def, "X");

        assertEquals("X", edge.from());
        assertEquals("Y", edge.to());
        assertEquals("提交", edge.event());
        assertNull(edge.when());
    }

    @Test
    void joinFlowOnWritesEventNotOutcome() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("发起");
        flow.node("发起", service("createReq"));
        flow.node("休假", b -> b.type(BuiltInNodeType.END_TASK));

        flow.from("发起").on("提交").to("审批");
        flow.from("审批")
                .join(Dsl.all(Dsl.node("HR", approval("hr")), Dsl.node("主管", approval("manager"))))
                .on("审批通过")
                .to("休假")
                .on("审批拒绝")
                .to("发起");

        WorkflowDefinition def = flow.build();

        List<EdgeDefinition> fromJoin =
                def.edges().stream().filter(e -> "审批".equals(e.from())).toList();
        assertEquals(2, fromJoin.size());

        EdgeDefinition approved =
                fromJoin.stream().filter(e -> "审批通过".equals(e.event())).findFirst().orElseThrow();
        assertEquals("休假", approved.to());

        EdgeDefinition rejected =
                fromJoin.stream().filter(e -> "审批拒绝".equals(e.event())).findFirst().orElseThrow();
        assertEquals("发起", rejected.to());

    }

    // E8 validation tests -----------------------------------------------------------------------

    @Test
    void joinOnNodeAlreadyRegisteredAsNonTaskGroup_throws() {
        FlowDef flow = Flow.define("w", "W");
        flow.node("X", service("doX")); // registered as serviceTask
        // Trying to turn X into a taskGroup via join() must throw
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                flow.from("X")
                                        .join(Dsl.all(Dsl.node("child", approval("hr")))));
        assertTrue(ex.getMessage().contains("X"));
        assertTrue(ex.getMessage().contains("serviceTask"));
        assertTrue(ex.getMessage().contains("taskGroup"));
    }

    @Test
    void joinOnFreshNode_works() {
        // X has not been registered yet → join() should succeed
        FlowDef flow = Flow.define("w", "W");
        flow.start("start");
        flow.node("start", service("init"));
        flow.node("done", b -> b.type(BuiltInNodeType.END_TASK));
        flow.from("start").on("submit").to("X");
        flow.from("X")
                .join(Dsl.all(Dsl.node("child1", approval("hr"))))
                .on("APPROVED")
                .to("done");
        WorkflowDefinition def = flow.build();
        // X must have been registered as taskGroup
        assertEquals("taskGroup", def.nodes().stream()
                .filter(n -> "X".equals(n.id())).findFirst().orElseThrow().type());
    }

    @Test
    void joinOnExistingTaskGroupNode_allowed() {
        // X was previously registered explicitly as taskGroup → join() is allowed (overwrite spec)
        FlowDef flow = Flow.define("w", "W");
        flow.node("X", b -> b.type(BuiltInNodeType.TASK_GROUP));
        flow.node("done", b -> b.type(BuiltInNodeType.END_TASK));
        flow.from("X")
                .join(Dsl.all(Dsl.node("child1", approval("hr"))))
                .on("APPROVED")
                .to("done");
        WorkflowDefinition def = flow.build();
        assertEquals("taskGroup", def.nodes().stream()
                .filter(n -> "X".equals(n.id())).findFirst().orElseThrow().type());
    }

    @Test
    void whenChainWritesGuardExpression() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("X");
        flow.node("X", service("doX"));
        flow.node("Y", b -> b.type(BuiltInNodeType.END_TASK));
        flow.from("X").on("提交").when("${ctx.variables.flag == true}").to("Y");

        WorkflowDefinition def = flow.build();
        EdgeDefinition edge = firstEdgeFrom(def, "X");

        assertEquals("提交", edge.event());
        assertEquals("${ctx.variables.flag == true}", edge.when());
    }
}
