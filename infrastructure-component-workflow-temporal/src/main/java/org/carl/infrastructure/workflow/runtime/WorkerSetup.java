package org.carl.infrastructure.workflow.runtime;

import io.temporal.worker.Worker;

import org.carl.infrastructure.workflow.archive.ArchiveActivities;
import org.carl.infrastructure.workflow.archive.WorkflowArchiverWorkflow;
import org.carl.infrastructure.workflow.archive.WorkflowArchiverWorkflowImpl;
import org.carl.infrastructure.workflow.spi.NodeHandlerRegistry;

import java.util.Objects;

/**
 * Convenience bootstrap for wiring up a {@link Worker} with the V2 runtime classes.
 *
 * <p>Side effects:
 *
 * <ol>
 *   <li>Registers {@link GenericWorkflowImpl} as the worker's workflow type.
 *   <li>Registers a {@link GenericActivityImpl} bound to {@code activityRegistry}.
 *   <li>Installs {@code handlerRegistry} into {@link HandlerHolder} so workflow code can resolve
 *       handlers via the static holder (Temporal forbids capturing worker-scope objects in workflow
 *       code).
 * </ol>
 *
 * <p>All three steps must happen before the worker is started.
 */
public final class WorkerSetup {

    private WorkerSetup() {
        throw new AssertionError("no instances");
    }

    public static void setup(
            Worker worker,
            NodeHandlerRegistry handlerRegistry,
            BusinessActivityRegistry activityRegistry) {
        setup(worker, handlerRegistry, activityRegistry, null);
    }

    /**
     * Extended setup with optional archival support.
     *
     * @param worker the Temporal worker
     * @param handlerRegistry the node handler registry
     * @param activityRegistry the business activity registry
     * @param archiveActivities optional archival activity implementation; if {@code null}, archival
     *     is disabled and workflows will not persist their termination data
     */
    public static void setup(
            Worker worker,
            NodeHandlerRegistry handlerRegistry,
            BusinessActivityRegistry activityRegistry,
            ArchiveActivities archiveActivities) {
        Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(handlerRegistry, "handlerRegistry");
        Objects.requireNonNull(activityRegistry, "activityRegistry");
        // archiveActivities is optional

        HandlerHolder.install(handlerRegistry);
        worker.registerWorkflowImplementationTypes(GenericWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GenericActivityImpl(activityRegistry));

        if (archiveActivities != null) {
            GenericWorkflowImpl.enableArchival();
            worker.registerWorkflowImplementationTypes(WorkflowArchiverWorkflowImpl.class);
            worker.registerActivitiesImplementations(archiveActivities);
        }
    }
}
