package org.carl.infrastructure.workflow.graph;

import org.carl.infrastructure.workflow.definition.EdgeDefinition;

import java.util.Objects;

/**
 * Matcher used by {@link WorkflowGraph#nextCandidates(String, EdgeMatch)} to describe how outgoing
 * edges should be filtered.
 *
 * <p>Three matching strategies are supported via the static factories:
 *
 * <ul>
 *   <li>{@link #byEvent(String)}: pick edges whose {@link EdgeDefinition#event()} equals the given
 *       event name.
 *   <li>{@link #byOutcome(String)}: pick edges whose {@link EdgeDefinition#outcome()} equals the
 *       given outcome.
 *   <li>{@link #any()}: pick all outgoing edges, regardless of event/outcome.
 * </ul>
 *
 * <p>Note: {@link EdgeDefinition#when()} is intentionally not evaluated by the graph layer because
 * the graph holds no execution context.
 */
public sealed interface EdgeMatch {

    static EdgeMatch byEvent(String eventName) {
        return new ByEvent(eventName);
    }

    static EdgeMatch byOutcome(String outcome) {
        return new ByOutcome(outcome);
    }

    static EdgeMatch any() {
        return Any.INSTANCE;
    }

    record ByEvent(String eventName) implements EdgeMatch {
        public ByEvent {
            Objects.requireNonNull(eventName, "eventName");
        }
    }

    /**
     * Legacy outcome-based matching. New code should use {@link #byEvent(String)}; the DSL now
     * writes node outcomes into the event field via outcome-event synthesis at the runtime layer.
     */
    @Deprecated(since = "v3", forRemoval = false)
    record ByOutcome(String outcome) implements EdgeMatch {
        public ByOutcome {
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    enum Any implements EdgeMatch {
        INSTANCE
    }
}
