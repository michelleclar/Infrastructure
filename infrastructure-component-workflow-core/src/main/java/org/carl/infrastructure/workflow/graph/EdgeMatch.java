package org.carl.infrastructure.workflow.graph;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;

import java.util.Objects;

/**
 * Matcher used by {@link WorkflowGraph#nextCandidates(String, EdgeMatch)} to describe how outgoing
 * edges should be filtered.
 *
 * <p>Two matching strategies are supported via the static factories:
 *
 * <ul>
 *   <li>{@link #byEvent(String)}: pick edges whose {@link EdgeDefinition#event()} equals the given
 *       event name.
 *   <li>{@link #any()}: pick all outgoing edges, regardless of event.
 * </ul>
 *
 * <p>Note: {@link EdgeDefinition#when()} is intentionally not evaluated by the graph layer because
 * the graph holds no execution context.
 */
public sealed interface EdgeMatch {

    /**
     * Matches outgoing edges whose {@link org.carl.infrastructure.workflow.definition.EdgeDefinition#event()}
     * equals {@code eventName}. This is the primary routing strategy used by the event-driven
     * runtime; outcome-based routing is handled at the runtime layer before reaching the graph.
     */
    static EdgeMatch byEvent(String eventName) {
        return new ByEvent(eventName);
    }

    /** Matches all outgoing edges unconditionally; used for reachability traversal. */
    static EdgeMatch any() {
        return Any.INSTANCE;
    }

    record ByEvent(String eventName) implements EdgeMatch {
        public ByEvent {
            Objects.requireNonNull(eventName, "eventName");
        }
    }

    enum Any implements EdgeMatch {
        INSTANCE
    }
}
