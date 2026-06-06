package org.carl.infrastructure.workflow.archive;

import io.temporal.workflow.Workflow;

import org.carl.infrastructure.workflow.runtime.WorkflowInstanceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link WorkflowArchiverWorkflow} that delegates to the archival activity.
 *
 * <p>When an {@code archive} signal is received, this workflow invokes {@link
 * ArchiveActivities#archive} to write the snapshot to the database.
 *
 * <p>Activity failures are automatically retried by Temporal. The workflow completes once the
 * archival activity succeeds.
 */
public class WorkflowArchiverWorkflowImpl implements WorkflowArchiverWorkflow {

    private static final Logger log = LoggerFactory.getLogger(WorkflowArchiverWorkflowImpl.class);

    private WorkflowInstanceSnapshot snapshot;
    private boolean signalReceived = false;

    @Override
    public boolean execute() {
        // Wait for the archive signal to be received
        Workflow.await(() -> signalReceived);

        log.info("Archiver processing archival for workflow: {}", snapshot.workflowId());
        // Delegate to activity to perform the actual database write
        Workflow.newActivityStub(ArchiveActivities.class).archive(snapshot);
        log.info("Archival completed for workflow: {}", snapshot.workflowId());

        return true;
    }

    @Override
    public void archive(WorkflowInstanceSnapshot snapshot) {
        log.info("Archiver received archive signal for workflow: {}", snapshot.workflowId());
        this.snapshot = snapshot;
        this.signalReceived = true;
    }
}
