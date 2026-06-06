package org.carl.infrastructure.workflow.definition;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.carl.infrastructure.workflow.spi.Outcomes;

import java.util.Map;
import java.util.Objects;

/**
 * Execution result of a single node run.
 *
 * <p>The canonical constructor wraps {@code payload} via {@link Map#copyOf} and substitutes {@link
 * Map#of()} when {@code null} is passed.
 *
 * <p>Static factories cover the common construction patterns:
 *
 * <ul>
 *   <li>{@link #waiting()}: node is suspended, no outcome yet.
 *   <li>{@link #completed(String)}, {@link #completed(String, Map)}: node finished with an outcome.
 *   <li>{@link #failed(String)}: node failed; outcome is {@link Outcomes#FAILED}.
 *   <li>{@link #cancelled()}: node cancelled; outcome is {@link Outcomes#CANCELLED}.
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NodeResult(
        NodeStatus status, String outcome, Map<String, Object> payload, String message) {

    public NodeResult {
        Objects.requireNonNull(status, "status");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static NodeResult waiting() {
        return new NodeResult(NodeStatus.WAITING, null, Map.of(), null);
    }

    public static NodeResult completed(String outcome) {
        return new NodeResult(NodeStatus.COMPLETED, outcome, Map.of(), null);
    }

    public static NodeResult completed(String outcome, Map<String, Object> payload) {
        return new NodeResult(NodeStatus.COMPLETED, outcome, payload, null);
    }

    public static NodeResult failed(String message) {
        return new NodeResult(NodeStatus.FAILED, Outcomes.FAILED, Map.of(), message);
    }

    public static NodeResult cancelled() {
        return new NodeResult(NodeStatus.CANCELLED, Outcomes.CANCELLED, Map.of(), null);
    }
}
