package org.carl.infrastructure.workflow.dsl;

import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.approval;
import static org.carl.infrastructure.workflow.dsl.Dsl.all;
import static org.carl.infrastructure.workflow.dsl.Dsl.any;
import static org.carl.infrastructure.workflow.dsl.Dsl.node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;
import org.carl.infrastructure.workflow.spi.Outcomes;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link FlowDef#buildTaskGroupConfig} recursively serialises a nested
 * {@link ChildNodeSpec#nestedJoin()} as a child {@code taskGroup} JSON node.
 *
 * <p>Scenario: outer ALL taskGroup with two children:
 * <ul>
 *   <li>HR approval (leaf, approvalTask)</li>
 *   <li>Management layer (nested ANY taskGroup with manager + CFO)</li>
 * </ul>
 */
class NestedTaskGroupJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Build the nested flow definition and inspect the serialised JSON of the outer taskGroup node.
     *
     * <pre>
     * approvals (taskGroup ALL)
     *   ├── hrApproval (approvalTask, "hr")
     *   └── managementApproval (taskGroup ANY)
     *       ├── managerApproval (approvalTask, "manager")
     *       └── cfoApproval    (approvalTask, "cfo")
     * </pre>
     */
    @Test
    void nestedTaskGroupNodeSerializesCorrectly() {
        WorkflowDefinition def = NestedLeaveProcess.nestedApprovalFlow();

        // Find the "approvals" node definition.
        NodeDefinition approvals =
                def.nodes().stream()
                        .filter(n -> "approvals".equals(n.id()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("approvals node not found"));

        assertEquals(NodeTypes.TASK_GROUP, approvals.type());

        JsonNode config = approvals.config();
        assertNotNull(config, "approvals config must not be null");

        // join.type == "all"
        JsonNode joinNode = config.get("join");
        assertNotNull(joinNode, "join field required");
        assertEquals("all", joinNode.get("type").asText());

        // tasks array: 2 children
        JsonNode tasks = config.get("tasks");
        assertNotNull(tasks, "tasks array required");
        assertTrue(tasks.isArray());
        assertEquals(2, tasks.size(), "should have 2 top-level tasks");

        // First child: hrApproval leaf
        JsonNode hr = tasks.get(0);
        assertEquals("hrApproval", hr.get("id").asText());
        assertEquals(NodeTypes.APPROVAL_TASK, hr.get("type").asText());
        assertNotNull(hr.get("config"), "hrApproval config should not be null");

        // Second child: managementApproval — must be a nested taskGroup
        JsonNode mgmt = tasks.get(1);
        assertEquals("managementApproval", mgmt.get("id").asText());
        assertEquals(NodeTypes.TASK_GROUP, mgmt.get("type").asText(),
                "managementApproval must be serialised as taskGroup");

        JsonNode mgmtConfig = mgmt.get("config");
        assertNotNull(mgmtConfig, "managementApproval config must be present");

        // Nested join: ANY
        JsonNode nestedJoin = mgmtConfig.get("join");
        assertNotNull(nestedJoin, "nested join field required");
        assertEquals("any", nestedJoin.get("type").asText());

        // Nested tasks: 2 leaves
        JsonNode nestedTasks = mgmtConfig.get("tasks");
        assertNotNull(nestedTasks, "nested tasks array required");
        assertTrue(nestedTasks.isArray());
        assertEquals(2, nestedTasks.size(), "managementApproval should have 2 leaf tasks");

        JsonNode manager = nestedTasks.get(0);
        assertEquals("managerApproval", manager.get("id").asText());
        assertEquals(NodeTypes.APPROVAL_TASK, manager.get("type").asText());

        JsonNode cfo = nestedTasks.get(1);
        assertEquals("cfoApproval", cfo.get("id").asText());
        assertEquals(NodeTypes.APPROVAL_TASK, cfo.get("type").asText());
    }

    /**
     * Verify three-level nesting: outermost ALL → middle ANY → innermost ALL.
     * Checks that recursion works beyond depth-2.
     */
    @Test
    void threeLayerNestingSerializesCorrectly() {
        // innermost: ALL [a, b]
        JoinSpec innermostSpec = all(
                node("a", approval("a")),
                node("b", approval("b")));

        // middle: ANY [c, inner-taskGroup]
        JoinSpec middleSpec = any(
                node("c", approval("c")),
                node("inner", new NodeConfig(NodeTypes.TASK_GROUP, java.util.Map.of()), innermostSpec));

        // outer: ALL [d, middle-taskGroup]
        FlowDef flow = Flow.define("threeLayer", "three layer");
        flow.start("start");
        flow.node("start", BuiltInNodes.service("noop"));
        flow.from("start").on(Outcomes.SUCCESS).to("outerGroup");
        flow.from("outerGroup")
                .join(all(
                        node("d", approval("d")),
                        node("middleGroup", new NodeConfig(NodeTypes.TASK_GROUP, java.util.Map.of()), middleSpec)))
                .on(Outcomes.APPROVED).to("done");
        flow.endNode("done");

        WorkflowDefinition def = flow.build();

        NodeDefinition outerNode =
                def.nodes().stream()
                        .filter(n -> "outerGroup".equals(n.id()))
                        .findFirst()
                        .orElseThrow();

        JsonNode outerConfig = outerNode.config();
        JsonNode outerTasks = outerConfig.get("tasks");
        assertEquals(2, outerTasks.size());

        // Second child is middle taskGroup
        JsonNode middleTask = outerTasks.get(1);
        assertEquals("middleGroup", middleTask.get("id").asText());
        assertEquals(NodeTypes.TASK_GROUP, middleTask.get("type").asText());

        JsonNode middleConfig = middleTask.get("config");
        assertEquals("any", middleConfig.get("join").get("type").asText());

        JsonNode middleTasks = middleConfig.get("tasks");
        assertEquals(2, middleTasks.size());

        // inner-taskGroup within middle
        JsonNode innerTask = middleTasks.get(1);
        assertEquals("inner", innerTask.get("id").asText());
        assertEquals(NodeTypes.TASK_GROUP, innerTask.get("type").asText());

        JsonNode innerConfig = innerTask.get("config");
        assertEquals("all", innerConfig.get("join").get("type").asText());
        assertEquals(2, innerConfig.get("tasks").size());
    }
}
