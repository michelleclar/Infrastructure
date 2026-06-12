package org.carl.infrastructure.workflow.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.NodeResult;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.dsl.BuiltInNodes;
import org.carl.infrastructure.workflow.dsl.Dsl;
import org.carl.infrastructure.workflow.dsl.Flow;
import org.carl.infrastructure.workflow.dsl.FlowDef;
import org.carl.infrastructure.workflow.handlers.BuiltInHandlers;
import org.carl.infrastructure.workflow.spi.BuiltInNodeType;
import org.carl.infrastructure.workflow.spi.NodeExecutionContext;
import org.carl.infrastructure.workflow.spi.NodeHandler;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class GraphValidatorTest {

    private static NodeDefinition node(String id, String type) {
        return new NodeDefinition(id, null, type, null, null);
    }

    private static NodeDefinition node(String id, String type, JsonNode config) {
        return new NodeDefinition(id, null, type, null, config);
    }

    private static EdgeDefinition event(String from, String to, String event) {
        return new EdgeDefinition(from, to, event, null);
    }

    @Test
    void validWorkflowPasses() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "leave",
                        "Leave",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "submit")));
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok(), () -> "expected no errors but got: " + report.errors());
        assertTrue(report.warnings().isEmpty(), "no warnings expected");
    }

    @Test
    void duplicateNodeIdReportsError() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("A", NodeTypes.END_TASK)),
                        List.of());
        ValidationReport report = GraphValidator.validate(def);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream().anyMatch(s -> s.contains("duplicate")),
                () -> "expected 'duplicate' in errors: " + report.errors());
    }

    @Test
    void edgePointingToUnknownNodeReportsError() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of(event("A", "ghost", "submit")));
        ValidationReport report = GraphValidator.validate(def);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream()
                        .anyMatch(s -> s.contains("ghost") && s.contains("unknown")),
                () -> "expected unknown-node error: " + report.errors());
    }

    @Test
    void registryWithoutHandlerForTypeReportsError() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "submit")));
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        // intentionally empty
        ValidationReport report = GraphValidator.validate(def, registry);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream().anyMatch(s -> s.contains("no handler")),
                () -> "expected 'no handler' error: " + report.errors());
    }

    @Test
    void registryWithHandlerButInvalidConfigReportsError() {
        ObjectNode badConfig = JsonNodeFactory.instance.objectNode();
        badConfig.put("activity", 12345); // expects String per ServiceConfig.activity
        badConfig.put("retryLimit", "not-a-number"); // expects int

        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK, badConfig),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "submit")));
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(new ServiceTaskHandler());
        // end node: register as Void config so it does not pollute the assertions
        registry.register(new EndTaskHandler());

        ValidationReport report = GraphValidator.validate(def, registry);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream()
                        .anyMatch(s -> s.contains("config invalid") && s.contains("A")),
                () -> "expected 'config invalid' error: " + report.errors());
    }

    @Test
    void registryRejectsEdgeEventNotDeclaredByHandlerOutcomes() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "sucess")));
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(new ServiceTaskHandler());
        registry.register(new EndTaskHandler());

        ValidationReport report = GraphValidator.validate(def, registry);

        assertFalse(report.ok());
        assertTrue(
                report.errors().stream()
                        .anyMatch(s -> s.contains("sucess") && s.contains("handler outcomes")),
                () -> "expected handler-outcomes error: " + report.errors());
    }

    @Test
    void registryAcceptsDslTaskGroupConfigShapeAfterNormalization() {
        FlowDef flow = Flow.define("w", "W");
        flow.start("approvals");
        flow.node("done", b -> b.type(BuiltInNodeType.END_TASK));
        flow.from("approvals")
                .join(Dsl.all(Dsl.node("hr", BuiltInNodes.approval("hr"))))
                .on(Outcomes.APPROVED)
                .to("done");
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        BuiltInHandlers.registerAll(registry);

        ValidationReport report = GraphValidator.validate(flow.build(), registry);

        assertTrue(report.ok(), () -> "expected no errors but got: " + report.errors());
    }

    @Test
    void unreachableNodeIsWarning() {
        // 'orphan' has incoming from 'sibling' but neither sibling nor orphan are reachable
        // from 'start'. 'sibling' is also a start node (no incoming) but its closure does not
        // contain 'start' either; the validator computes reachability from all start nodes
        // and warns on any node not in the union — but since sibling/orphan are both start
        // nodes themselves they are reachable from a start. So we instead point 'orphan' at
        // a cycle-only subgraph whose root is not a start node.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("start", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK),
                                node("a", NodeTypes.SERVICE_TASK),
                                node("b", NodeTypes.SERVICE_TASK)),
                        // start -> end is the main path.
                        // a <-> b is a side cycle. Neither a nor b is a start node
                        // (both have incoming), and they are not reached by start.
                        List.of(
                                event("start", "end", "submit"),
                                event("a", "b", "x"),
                                event("b", "a", "y")));
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok(), () -> "expected ok but got errors: " + report.errors());
        assertTrue(
                report.warnings().stream()
                        .anyMatch(s -> s.contains("unreachable") && s.contains("a")),
                () -> "expected unreachable warning: " + report.warnings());
        assertTrue(
                report.warnings().stream()
                        .anyMatch(s -> s.contains("unreachable") && s.contains("b")),
                () -> "expected unreachable warning: " + report.warnings());
    }

    @Test
    void selfLoopIsWarning() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "A", "loop"), event("A", "end", "go")));
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok(), () -> "expected ok but got errors: " + report.errors());
        assertTrue(
                report.warnings().stream()
                        .anyMatch(s -> s.contains("self-loop") && s.contains("A")),
                () -> "expected self-loop warning: " + report.warnings());
    }

    @Test
    void timerTaskSelfLoopIsNotEvenAWarning() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("T", NodeTypes.TIMER_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("T", "T", "tick"), event("T", "end", "done")));
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok());
        assertFalse(
                report.warnings().stream().anyMatch(s -> s.contains("self-loop")),
                "timerTask self-loop is intentional and must not warn");
    }

    @Test
    void cycleIsWarning() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(
                                event("A", "B", "x"),
                                event("B", "A", "y"),
                                event("B", "end", "done")));
        ValidationReport report = GraphValidator.validate(def);
        // Cycle is a warning. Start nodes: none (both A and B have incoming). That triggers
        // the 'no start node' error. We acknowledge that and assert the warning is present.
        assertTrue(
                report.warnings().stream().anyMatch(s -> s.contains("cycle")),
                () -> "expected cycle warning: " + report.warnings());
    }

    @Test
    void noStartNodeReportsError() {
        // every node has incoming via the cycle
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK)),
                        List.of(event("A", "B", "x"), event("B", "A", "y")));
        ValidationReport report = GraphValidator.validate(def);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream().anyMatch(s -> s.contains("start node")),
                () -> "expected start-node error: " + report.errors());
    }

    @Test
    void explicitStartNodeIdAllowsBackEdgeToStart() {
        // Back-edge "B -> A" makes A have incoming. Without an explicit start the validator
        // rejects this (no node with zero incoming). With startNodeId="A" it must pass.
        WorkflowDefinition def =
                new WorkflowDefinition(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(
                                event("A", "B", "x"),
                                event("B", "A", "y"),
                                event("B", "end", "done")),
                        "A");
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok(), () -> "expected ok but got errors: " + report.errors());
    }

    @Test
    void multipleTopologicalStartsTriggerWarningButNoError() {
        // Two nodes with zero incoming, no explicit startNodeId -> warning, but still valid.
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(
                                node("A", NodeTypes.SERVICE_TASK),
                                node("B", NodeTypes.SERVICE_TASK),
                                node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "go"), event("B", "end", "go")));
        ValidationReport report = GraphValidator.validate(def);
        assertTrue(report.ok(), () -> "expected ok: " + report.errors());
        assertTrue(
                report.warnings().stream()
                        .anyMatch(s -> s.contains("multiple potential start nodes")),
                () -> "expected multiple-starts warning: " + report.warnings());
    }

    @Test
    void multipleIsolatedNodesReportsError() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("B", NodeTypes.END_TASK)),
                        List.of());
        ValidationReport report = GraphValidator.validate(def);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream().anyMatch(s -> s.contains("isolated")),
                () -> "expected isolated-nodes error: " + report.errors());
    }

    @Test
    void emptyNodesReportsError() {
        WorkflowDefinition def = WorkflowDefinition.of("w", "W", List.of(), List.of());
        ValidationReport report = GraphValidator.validate(def);
        assertFalse(report.ok());
        assertTrue(
                report.errors().stream().anyMatch(s -> s.contains("at least one node")),
                () -> "expected node-count error: " + report.errors());
    }

    @Test
    void throwIfInvalidThrowsWithAllErrors() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("A", NodeTypes.END_TASK)),
                        List.of(event("A", "ghost", "submit")));
        ValidationReport report = GraphValidator.validate(def);
        IllegalStateException ex =
                assertThrows(IllegalStateException.class, report::throwIfInvalid);
        // Must mention every error
        for (String e : report.errors()) {
            assertTrue(
                    ex.getMessage().contains(e),
                    () -> "missing error in message: '" + e + "' / " + ex.getMessage());
        }
    }

    @Test
    void throwIfInvalidIsNoOpWhenOk() {
        WorkflowDefinition def =
                WorkflowDefinition.of(
                        "w",
                        "W",
                        List.of(node("A", NodeTypes.SERVICE_TASK), node("end", NodeTypes.END_TASK)),
                        List.of(event("A", "end", "submit")));
        ValidationReport report = GraphValidator.validate(def);
        report.throwIfInvalid(); // must not throw
    }

    @Test
    void validateNodeConfigSucceedsForVoidHandler() {
        NodeDefinition n = node("A", NodeTypes.END_TASK);
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(new EndTaskHandler());
        List<String> errors = GraphValidator.validateNodeConfig(n, registry);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateNodeConfigSucceedsForValidConfig() {
        ObjectNode goodConfig = JsonNodeFactory.instance.objectNode();
        goodConfig.put("activity", "createLeaveRequest");
        goodConfig.put("retryLimit", 3);

        NodeDefinition n = node("A", NodeTypes.SERVICE_TASK, goodConfig);
        NodeHandlerRegistry registry = new NodeHandlerRegistry();
        registry.register(new ServiceTaskHandler());

        List<String> errors = GraphValidator.validateNodeConfig(n, registry);
        assertTrue(errors.isEmpty(), () -> "unexpected errors: " + errors);
    }

    // ----- test handlers -----

    /** Config bean with a couple of typed fields so Jackson can fail when types mismatch. */
    static final class ServiceConfig {
        public String activity;
        public int retryLimit;
    }

    static final class ServiceTaskHandler implements NodeHandler<ServiceConfig, Object, Object> {
        @Override
        public String type() {
            return NodeTypes.SERVICE_TASK;
        }

        @Override
        public Class<ServiceConfig> configType() {
            return ServiceConfig.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.SUCCESS, Outcomes.FAILED);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, ServiceConfig config) {
            return NodeResult.completed(Outcomes.SUCCESS);
        }
    }

    static final class EndTaskHandler implements NodeHandler<Void, Object, Object> {
        @Override
        public String type() {
            return NodeTypes.END_TASK;
        }

        @Override
        public Class<Void> configType() {
            return Void.class;
        }

        @Override
        public Set<String> outcomes() {
            return Set.of(Outcomes.COMPLETED);
        }

        @Override
        public NodeResult run(NodeExecutionContext ctx, Void config) {
            return NodeResult.completed(Outcomes.COMPLETED);
        }
    }
}
