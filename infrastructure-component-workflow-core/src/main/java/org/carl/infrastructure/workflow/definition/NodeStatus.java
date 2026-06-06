package org.carl.infrastructure.workflow.definition;

/**
 * Lifecycle status of a workflow node execution.
 *
 * <p>Strictly four values; do not extend without updating the runtime contract.
 */
public enum NodeStatus {
    WAITING,
    COMPLETED,
    FAILED,
    CANCELLED
}
