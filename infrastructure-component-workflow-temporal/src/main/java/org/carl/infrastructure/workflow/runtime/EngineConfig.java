package org.carl.infrastructure.workflow.runtime;

import java.util.Objects;

/**
 * Connection settings for {@link WorkflowEngine}. Plain strings — no Temporal types — so business
 * code configures the engine without importing {@code io.temporal.*}.
 *
 * @param target Temporal frontend address, e.g. {@code "localhost:7233"} or {@code "host:port"}.
 * @param namespace Temporal namespace; blank/null defaults to {@code "default"}.
 * @param taskQueue task queue the worker polls and workflows are dispatched to.
 */
public record EngineConfig(String target, String namespace, String taskQueue) {

    public EngineConfig {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(taskQueue, "taskQueue");
        if (namespace == null || namespace.isBlank()) {
            namespace = "default";
        }
    }

    /** Config with the {@code "default"} namespace. */
    public static EngineConfig of(String target, String taskQueue) {
        return new EngineConfig(target, "default", taskQueue);
    }
}
