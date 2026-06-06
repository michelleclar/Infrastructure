package org.carl.infrastructure.workflow.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.carl.infrastructure.workflow.definition.ExecutionRecord;
import org.carl.infrastructure.workflow.runtime.WorkflowInstanceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ArchiveActivities} for testing.
 *
 * <p>Production implementations should use {@code IPersistenceOperations} to write to the database.
 * This class stores snapshots in memory and is suitable only for test environments.
 *
 * <p>Thread-safe for concurrent test execution.
 */
public class InMemoryArchiveActivities implements ArchiveActivities {

    private static final Logger log = LoggerFactory.getLogger(InMemoryArchiveActivities.class);
    private static final Map<String, WorkflowInstanceSnapshot> STORAGE = new ConcurrentHashMap<>();

    @Override
    public void archive(WorkflowInstanceSnapshot snapshot) {
        log.info(
                "Archiving workflow: workflowId={}, status={}, finalNodeId={}",
                snapshot.workflowId(),
                snapshot.status(),
                snapshot.finalNodeId());
        STORAGE.put(snapshot.workflowId(), snapshot);
    }

    /**
     * Retrieve a snapshot by workflow ID. Returns {@code null} if not found.
     *
     * <p>Test-only helper method.
     */
    public static WorkflowInstanceSnapshot get(String workflowId) {
        return STORAGE.get(workflowId);
    }

    /** Clear all snapshots. Test-only helper method. */
    public static void clear() {
        STORAGE.clear();
    }

    /** Check if a snapshot exists. Test-only helper method. */
    public static boolean contains(String workflowId) {
        return STORAGE.containsKey(workflowId);
    }
}
