package org.carl.infrastructure.workflow.runtime;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Runtime config for workflow. */
@ConfigMapping(prefix = "workflow")
public interface WorkflowConfig {

    /** Dedicated Temporal task queue for the workflow engine (kept separate to avoid clashes). */
    @WithDefault("workflow-tasks")
    String taskQueue();
}
