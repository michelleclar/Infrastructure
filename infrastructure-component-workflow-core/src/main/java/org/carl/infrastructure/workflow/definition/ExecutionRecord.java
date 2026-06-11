package org.carl.infrastructure.workflow.definition;

/**
 * Ordered history entry for a single node visit.
 *
 * <p>{@code nodeId} is the top-level node id or the qualified task-group child id ({@code
 * parent/child}). {@code visitNo} starts at 1 and increments when the same node is reached again
 * through a back-edge. {@code result} is the exact node result recorded for that visit.
 */
public record ExecutionRecord(String nodeId, int visitNo, NodeResult result) {}
