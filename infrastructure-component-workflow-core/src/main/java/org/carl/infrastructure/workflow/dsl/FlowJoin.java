package org.carl.infrastructure.workflow.dsl;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;

import java.util.Objects;

/**
 * Builder returned by {@link FlowFrom#join(JoinSpec)}, enabling fluent {@code .on(event).to(dest)}
 * chaining after a join declaration.
 *
 * <pre>
 * flow.from("发起请假审批")
 *     .join(all(...))
 *     .on("审批通过").to("休假")
 *     .on("审批拒绝").to("发起请假");
 * </pre>
 *
 * <p><strong>Edge-routing semantics:</strong> the DSL writes the argument of {@link #on(String)}
 * into {@link EdgeDefinition#event()}; {@link EdgeDefinition#outcome()} is always {@code null} for
 * edges produced through this builder. See {@link FlowFrom} for the full discussion.
 */
public final class FlowJoin {

    private final FlowDef flowDef;
    private final String fromNodeName;

    FlowJoin(FlowDef flowDef, String fromNodeName) {
        this.flowDef = flowDef;
        this.fromNodeName = fromNodeName;
    }

    /**
     * Begins an event-triggered edge from the join node. The supplied name is stored on the
     * produced {@link EdgeDefinition#event()} field.
     *
     * @param event the event name (e.g. {@code "审批通过"})
     * @return a {@link FlowJoinTo} to complete the edge with {@code .to(dest)}
     */
    public FlowJoinTo on(String event) {
        Objects.requireNonNull(event, "event");
        return new FlowJoinTo(this, event);
    }

    /** Registers an edge on the parent {@link FlowDef}. Called by {@link FlowJoinTo#to}. */
    void registerEdge(String event, String dest) {
        flowDef.registerEdge(new EdgeDefinition(fromNodeName, dest, event, null));
    }

    /** Intermediate builder that captures the event and waits for the destination. */
    public static final class FlowJoinTo {

        private final FlowJoin parent;
        private final String event;

        private FlowJoinTo(FlowJoin parent, String event) {
            this.parent = parent;
            this.event = event;
        }

        /**
         * Completes the edge by specifying the destination node and returns the parent {@link
         * FlowJoin} for further {@code .on().to()} chaining.
         *
         * @param dest the target node name
         * @return the parent {@link FlowJoin}
         */
        public FlowJoin to(String dest) {
            Objects.requireNonNull(dest, "dest");
            parent.registerEdge(event, dest);
            return parent;
        }
    }
}
