package org.carl.infrastructure.workflow.dsl;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;
import org.carl.infrastructure.workflow.spi.NodeTypes;

import java.util.Objects;

/**
 * Edge-construction step returned by {@link FlowDef#from(String)}.
 *
 * <p>Three paths are available:
 *
 * <ul>
 *   <li>{@link #on(String)} — simple event-triggered edge: {@code .on(event).to(dest)}
 *   <li>{@link #on(String)}.{@link FlowTo#when(String)} — guarded edge: {@code
 *       .on(event).when(elExpression).to(dest)}
 *   <li>{@link #join(JoinSpec)} — register a task-group join spec and get a {@link FlowJoin} for
 *       chaining {@code .on(event).to(dest)} pairs
 * </ul>
 *
 * <p><strong>Edge-routing semantics:</strong> the DSL writes the argument of {@link #on(String)}
 * into {@link EdgeDefinition#event()}. The runtime compares a node's completed outcome with that
 * field when choosing the next edge.
 *
 * <p>The optional {@link FlowTo#when(String)} step writes an EL guard expression into {@link
 * EdgeDefinition#when()}.
 */
public final class FlowFrom {

    private final FlowDef flowDef;
    private final String fromNodeName;

    FlowFrom(FlowDef flowDef, String fromNodeName) {
        this.flowDef = flowDef;
        this.fromNodeName = fromNodeName;
    }

    /**
     * Begins an event-triggered edge from the source node. The supplied name is stored on the
     * produced {@link EdgeDefinition#event()} field.
     *
     * @param event the event name that activates the edge
     * @return a {@link FlowTo} to optionally add a guard with {@link FlowTo#when(String)} and
     *     complete the edge with {@code .to(dest)}
     */
    public FlowTo on(String event) {
        Objects.requireNonNull(event, "event");
        return new FlowTo(this, event);
    }

    /**
     * Registers a task-group join specification on the source node and returns a {@link FlowJoin}
     * for chaining event-triggered edges.
     *
     * <p>The source node is (re-)registered in the parent {@link FlowDef} as a {@code taskGroup}
     * placeholder (if not already present), and the {@link JoinSpec} is stored so that {@link
     * FlowDef#build()} can compile it into the correct JSON config shape.
     *
     * @param spec the join spec produced by {@link Dsl#all} or {@link Dsl#any}
     * @return a {@link FlowJoin} for chaining {@code .on(event).to(dest)} pairs
     */
    public FlowJoin join(JoinSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String conflictingType = flowDef.registeredNonTaskGroupType(fromNodeName);
        if (conflictingType != null) {
            throw new IllegalStateException(
                    "node '"
                            + fromNodeName
                            + "' is already registered as type '"
                            + conflictingType
                            + "'; cannot turn it into a taskGroup via flow.from().join()");
        }
        flowDef.registerJoinSpec(fromNodeName, spec);
        flowDef.ensureNode(
                fromNodeName,
                new org.carl.infrastructure.workflow.dsl.NodeConfig(
                        NodeTypes.TASK_GROUP, java.util.Map.of()));
        return new FlowJoin(flowDef, fromNodeName);
    }

    /** Registers an edge (no guard) on the parent {@link FlowDef}. Called by {@link FlowTo#to}. */
    void registerEdge(String event, String dest) {
        flowDef.registerEdge(new EdgeDefinition(fromNodeName, dest, event, null));
    }

    /** Registers a guarded edge on the parent {@link FlowDef}. Called by {@link WhenFlowTo#to}. */
    void registerEdge(String event, String dest, String when) {
        flowDef.registerEdge(new EdgeDefinition(fromNodeName, dest, event, when));
    }

    /**
     * Intermediate builder that captures the event and waits for the destination (or an optional
     * guard expression).
     */
    public static final class FlowTo {

        private final FlowFrom parent;
        private final String event;

        private FlowTo(FlowFrom parent, String event) {
            this.parent = parent;
            this.event = event;
        }

        /**
         * Adds an optional EL guard expression on this edge. The supplied expression is stored in
         * {@link EdgeDefinition#when()}.
         *
         * @param elExpression the EL guard (e.g. {@code "${ctx.amount > 10000}"})
         * @return a {@link WhenFlowTo} to complete the edge with {@code .to(dest)}
         */
        public WhenFlowTo when(String elExpression) {
            Objects.requireNonNull(elExpression, "elExpression");
            return new WhenFlowTo(parent, event, elExpression);
        }

        /**
         * Completes the edge by specifying the destination node.
         *
         * @param dest the target node name
         */
        public void to(String dest) {
            Objects.requireNonNull(dest, "dest");
            parent.registerEdge(event, dest);
        }
    }

    /**
     * Intermediate builder that holds both the event and a guard expression, waiting only for the
     * destination node.
     */
    public static final class WhenFlowTo {

        private final FlowFrom parent;
        private final String event;
        private final String when;

        private WhenFlowTo(FlowFrom parent, String event, String when) {
            this.parent = parent;
            this.event = event;
            this.when = when;
        }

        /**
         * Completes the edge by specifying the destination node.
         *
         * @param dest the target node name
         */
        public void to(String dest) {
            Objects.requireNonNull(dest, "dest");
            parent.registerEdge(event, dest, when);
        }
    }
}
