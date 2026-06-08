package org.carl.infrastructure.workflow.archive;

import io.temporal.activity.ActivityInterface;

import org.carl.infrastructure.workflow.runtime.WorkflowInstanceSnapshot;

/**
 * Activity interface for writing workflow archival data to the database.
 *
 * <p>Implemented by {@link DatabaseArchiveActivities} (production). The runtime fires this
 * activity asynchronously via {@code Async.procedure} on terminal completion when the per-execution
 * {@code WorkflowInput.archive} flag is set; failures are best-effort and never affect the parent
 * workflow result.
 *
 * <p>Production implementations should write to {@code workflow_instance} and {@code
 * execution_record} database tables.
 */
@ActivityInterface
public interface ArchiveActivities {

    /**
     * Archive the given workflow snapshot to the database.
     *
     * <p>Writes to {@code workflow_instance} and {@code execution_record} tables. Uses ON CONFLICT
     * for idempotency.
     *
     * @param snapshot the terminated workflow instance data
     */
    void archive(WorkflowInstanceSnapshot snapshot);
}
