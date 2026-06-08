package org.carl.infrastructure.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.definition.NodeDefinition;
import org.carl.infrastructure.workflow.definition.WorkflowDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.function.Consumer;

/**
 * Accumulator returned by {@link Flow#define(String, String)}.
 *
 * <p>Collect nodes, edges, and join specs via the fluent API, then call {@link #build()} to produce
 * an immutable {@link WorkflowDefinition}.
 *
 * <h2>Flow-first API synopsis</h2>
 *
 * <pre>
 * import static org.carl.infrastructure.workflow.dsl.BuiltInNodes.*;
 * import static org.carl.infrastructure.workflow.dsl.Dsl.*;
 *
 * FlowDef flow = Flow.define("leaveV2", "请假流程");
 * flow.start("发起请假");
 *
 * flow.node("发起请假", service("createLeaveRequest"));
 * flow.node("休假", endTask());
 *
 * flow.from("发起请假").on("success").to("发起请假审批");
 *
 * flow.from("发起请假审批")
 *     .join(all(
 *         node("HR 审批",    approval("hr")),
 *         node("部门主管审批", approval("manager"))
 *     ))
 *     .on("审批通过").to("休假")
 *     .on("审批拒绝").to("发起请假");
 *
 * WorkflowDefinition def = flow.build();
 * </pre>
 */
public final class FlowDef {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String workflowId;
    private final String workflowName;

    /** Explicit node configs, preserving insertion order. */
    private final Map<String, NodeConfig> nodeConfigs = new LinkedHashMap<>();

    /** Optional display label overrides keyed by node id, set via {@link NodeBuilder#label}. */
    private final Map<String, String> labels = new LinkedHashMap<>();

    /** Accumulated edges. */
    private final List<EdgeDefinition> edges = new ArrayList<>();

    /** Join specs keyed by the from-node name. Populated via {@link FlowFrom#join}. */
    private final Map<String, JoinSpec> joinSpecs = new LinkedHashMap<>();

    /** Optional pinned start node. */
    private String startNodeId;

    FlowDef(String workflowId, String workflowName) {
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId");
        this.workflowName = Objects.requireNonNull(workflowName, "workflowName");
    }

    // -------------------------------------------------------------------------
    // Public DSL methods
    // -------------------------------------------------------------------------

    /**
     * Pins the workflow entry node. Must refer to a node that will be present at {@link #build()}
     * time.
     */
    public FlowDef start(String name) {
        Objects.requireNonNull(name, "name");
        this.startNodeId = name;
        return this;
    }

    /**
     * Registers a named node with its {@link NodeConfig}.
     *
     * <p>If the node was already registered (e.g. as a placeholder by {@link FlowFrom#join}), the
     * config is overwritten only when the incoming config is not a bare {@code taskGroup}
     * placeholder — join specs take precedence.
     */
    public FlowDef node(String name, NodeConfig config) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(config, "config");
        nodeConfigs.put(name, config);
        return this;
    }

    /**
     * Registers a node by composing its {@link NodeConfig} through a {@link NodeBuilder}.
     *
     * <p>This is the extensibility entry point: any node type — built-in or business-defined — can
     * be declared without modifying {@code workflow-core}. Example with a custom handler:
     *
     * <pre>
     * flow.node("aiTriage", b -> b.type("aiReview").set("model", "gpt-4"));
     * </pre>
     *
     * <p>If {@link NodeBuilder#label(String)} is set on the builder, the value is stored as a
     * display-label override for the produced {@link NodeDefinition}.
     */
    public FlowDef node(String id, Consumer<NodeBuilder> configurer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(configurer, "configurer");
        NodeBuilder b = new NodeBuilder();
        configurer.accept(b);
        NodeConfig cfg = b.buildConfig();
        nodeConfigs.put(id, cfg);
        if (b.getLabel() != null) {
            labels.put(id, b.getLabel());
        }
        return this;
    }

    /** Returns a {@link FlowFrom} anchored on {@code name} for building outgoing edges. */
    public FlowFrom from(String name) {
        Objects.requireNonNull(name, "name");
        return new FlowFrom(this, name);
    }

    /**
     * Convenience method to register an end node.
     *
     * <p>Equivalent to {@code node(name, end())}.
     *
     * @param name the end node name
     * @return this FlowDef for chaining
     */
    public FlowDef endNode(String name) {
        Objects.requireNonNull(name, "name");
        return node(
                name,
                new org.carl.infrastructure.workflow.dsl.NodeConfig(NodeTypes.END_TASK, Map.of()));
    }

    // -------------------------------------------------------------------------
    // Package-private callbacks used by FlowFrom / FlowJoin
    // -------------------------------------------------------------------------

    /** Called by {@link FlowFrom#join} to store the join spec. */
    void registerJoinSpec(String nodeName, JoinSpec spec) {
        joinSpecs.put(nodeName, spec);
    }

    /** Called by {@link FlowFrom#join} to ensure the node appears in nodeConfigs. */
    void ensureNode(String name, NodeConfig config) {
        nodeConfigs.putIfAbsent(name, config);
    }

    /**
     * Returns the type string of {@code name} if the node is already registered in
     * {@link #nodeConfigs} as a type <em>other than</em> {@code taskGroup}, or {@code null}
     * otherwise (unregistered or already a taskGroup).
     *
     * <p>Used by {@link FlowFrom#join} to detect the misuse pattern where a node that was
     * already declared with meaningful config is later silently turned into a {@code taskGroup}.
     */
    String registeredNonTaskGroupType(String name) {
        NodeConfig existing = nodeConfigs.get(name);
        if (existing == null) {
            return null;
        }
        if (NodeTypes.TASK_GROUP.equals(existing.type())) {
            return null;
        }
        return existing.type();
    }

    /** Called by {@link FlowFrom.FlowTo} and {@link FlowJoin.FlowJoinTo} to store an edge. */
    void registerEdge(EdgeDefinition edge) {
        edges.add(edge);
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    /**
     * Compiles all accumulated state into an immutable {@link WorkflowDefinition}.
     *
     * <p>Build logic:
     *
     * <ol>
     *   <li>Collect all node names: explicit {@code nodeConfigs} keys + join children + implicit
     *       nodes that only appear as edge {@code .to()} targets.
     *   <li>For each node name: construct a {@link NodeDefinition} from its config. Nodes with a
     *       registered {@link JoinSpec} emit the {@code taskGroup} JSON shape understood by {@link
     *       org.carl.infrastructure.workflow.handlers.TaskGroupHandler}.
     *   <li>Nodes that appear only in edge targets with no explicit config are auto-typed as {@code
     *       endTask}.
     *   <li>Wrap everything in a {@link WorkflowDefinition}.
     * </ol>
     */
    public WorkflowDefinition build() {
        // Collect all node names we need to emit, preserving encounter order.
        SequencedSet<String> allNames = new LinkedHashSet<>(nodeConfigs.keySet());

        // Add child names from join specs (they appear in the taskGroup config, not as top-level
        // nodes).
        // They do NOT need a top-level NodeDefinition — keep this comment for clarity.

        // Add implicit nodes from edge targets.
        for (EdgeDefinition edge : edges) {
            allNames.add(edge.from());
            allNames.add(edge.to());
        }

        List<NodeDefinition> nodes = new ArrayList<>(allNames.size());

        for (String name : allNames) {
            NodeDefinition nd;
            String label = labels.getOrDefault(name, name);

            if (joinSpecs.containsKey(name)) {
                // This node is a taskGroup with a registered join spec — compile to full JSON.
                nd =
                        new NodeDefinition(
                                name,
                                label,
                                NodeTypes.TASK_GROUP,
                                null,
                                buildTaskGroupConfig(joinSpecs.get(name)));
            } else if (nodeConfigs.containsKey(name)) {
                NodeConfig cfg = nodeConfigs.get(name);
                nd =
                        new NodeDefinition(
                                name, label, cfg.type(), null, MAPPER.valueToTree(cfg.props()));
            } else {
                // Implicit node (only appeared in an edge target) — auto-type as endTask.
                nd =
                        new NodeDefinition(
                                name, label, NodeTypes.END_TASK, null, MAPPER.createObjectNode());
            }

            nodes.add(nd);
        }

        return new WorkflowDefinition(workflowId, workflowName, nodes, edges, startNodeId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the JSON config for a {@code taskGroup} node from its {@link JoinSpec}.
     *
     * <p>Produces:
     *
     * <pre>
     * {
     *   "join": {"type": "all"},
     *   "tasks": [
     *     {"id": "HR 审批", "label": "HR 审批", "type": "approvalTask", "config": {...}},
     *     {"id": "管理层", "label": "管理层", "type": "taskGroup",
     *      "config": {"join": {"type": "any"}, "tasks": [...]}},   // nested taskGroup
     *     ...
     *   ]
     * }
     * </pre>
     *
     * <p>When a child has a non-{@code null} {@link ChildNodeSpec#nestedJoin()}, it is serialised as
     * a nested {@code taskGroup}: {@code type="taskGroup"} and {@code config} is the recursively
     * built JSON produced by calling this method again on the nested {@link JoinSpec}. Leaf children
     * (where {@code nestedJoin} is {@code null}) are serialised directly from their {@link
     * ChildNodeSpec#config()}.
     */
    private static com.fasterxml.jackson.databind.JsonNode buildTaskGroupConfig(JoinSpec spec) {
        ObjectNode root = MAPPER.createObjectNode();

        ObjectNode join = root.putObject("join");
        join.put("type", spec.rule());

        ArrayNode tasks = root.putArray("tasks");
        for (ChildNodeSpec child : spec.children()) {
            ObjectNode task = tasks.addObject();
            task.put("id", child.name());
            task.put("label", child.name());
            if (child.nestedJoin() != null) {
                // Recursive case: this child is itself a taskGroup.
                task.put("type", NodeTypes.TASK_GROUP);
                task.set("config", buildTaskGroupConfig(child.nestedJoin()));
            } else {
                // Leaf case: serialise from the node config directly.
                task.put("type", child.config().type());
                task.set("config", MAPPER.valueToTree(child.config().props()));
            }
        }

        return root;
    }
}
