package org.carl.infrastructure.workflow.archive;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import org.carl.infrastructure.workflow.runtime.WorkflowInstanceSnapshot;

/**
 * Workflow that receives archival signals from terminated workflows and persists their snapshots to
 * the database.
 *
 * <p>Each workflow instance triggers archival by sending an {@code archive} signal to a dedicated
 * archiver workflow (named {@code <parentWorkflowId>-archiver}). The archiver workflow invokes
 * {@link ArchiveActivities#archive} to write to the database.
 *
 * <p>The archiver workflow lifecycle:
 *
 * <ol>
 *   <li>Waits for {@code archive} signal with {@link WorkflowInstanceSnapshot}
 *   <li>Invokes {@code ArchiveActivities.archive(snapshot)}
 *   <li>Returns {@code true} when archival completes
 * </ol>
 *
 * <p>Failures in archival activity are automatically retried by Temporal. If all retries are
 * exhausted, the archiver workflow fails, but this does not affect the parent workflow result.
 */
@WorkflowInterface
public interface WorkflowArchiverWorkflow {

    /**
     * Main workflow method that waits for the archive signal.
     *
     * @return {@code true} when archival completes successfully
     */
    @WorkflowMethod
    boolean execute();

    /**
     * Receives the archival signal from a terminated workflow.
     *
     * @param snapshot the workflow instance data to archive
     */
    @SignalMethod
    void archive(WorkflowInstanceSnapshot snapshot);
}
