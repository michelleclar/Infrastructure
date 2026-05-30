package org.carl.infrastructure.workflowv2.runtime;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Runtime config for workflowV2. */
@ConfigMapping(prefix = "workflowv2")
public interface WorkflowV2Config {

    /** Dedicated Temporal task queue for the workflowV2 engine (kept separate to avoid clashes). */
    @WithDefault("workflowv2-tasks")
    String taskQueue();
}
