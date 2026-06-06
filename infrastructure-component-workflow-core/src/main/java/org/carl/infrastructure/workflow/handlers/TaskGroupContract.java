package org.carl.infrastructure.workflow.handlers;

import java.util.Objects;

/**
 * Conventions shared between {@link TaskGroupHandler} and the workflow runtime for addressing
 * task-group child results.
 *
 * <p>The runtime is expected to store each child task's {@link
 * org.carl.infrastructure.workflow.definition.NodeResult} in {@link
 * org.carl.infrastructure.workflow.spi.NodeExecutionContext#resultOf(String)} under the qualifier
 * produced by {@link #childKey(String, String)}.
 */
public final class TaskGroupContract {

    /**
     * Compose the lookup key for a child task's result inside a parent task group.
     *
     * @param parentNodeId the task-group node id (non-null, non-blank)
     * @param childId the child task id (non-null, non-blank)
     * @return {@code parentNodeId + "/" + childId}
     */
    public static String childKey(String parentNodeId, String childId) {
        Objects.requireNonNull(parentNodeId, "parentNodeId");
        Objects.requireNonNull(childId, "childId");
        if (parentNodeId.isBlank()) {
            throw new IllegalArgumentException("parentNodeId must not be blank");
        }
        if (childId.isBlank()) {
            throw new IllegalArgumentException("childId must not be blank");
        }
        return parentNodeId + "/" + childId;
    }

    private TaskGroupContract() {
        throw new AssertionError("no instances");
    }
}
