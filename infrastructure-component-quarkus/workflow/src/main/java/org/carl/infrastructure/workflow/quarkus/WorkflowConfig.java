package org.carl.infrastructure.workflow.quarkus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration for the Quarkus workflow adapter (prefix {@code workflow}).
 *
 * <pre>
 * workflow.task-queue=workflow-tasks   # Temporal task queue the worker polls
 * workflow.start-worker=true           # false = client-only (start/signal/query), no local worker
 * </pre>
 */
@ConfigMapping(prefix = "workflow")
public interface WorkflowConfig {

    /** Dedicated Temporal task queue for the workflow engine (kept separate to avoid clashes). */
    @WithDefault("workflow-tasks")
    String taskQueue();

    /**
     * Whether this node starts a Temporal worker at boot. Set {@code false} for client-only
     * deployments that merely start/signal/query workflows whose worker runs elsewhere; {@link
     * WorkflowFacade} stays fully usable either way.
     */
    @WithDefault("true")
    boolean startWorker();
}
