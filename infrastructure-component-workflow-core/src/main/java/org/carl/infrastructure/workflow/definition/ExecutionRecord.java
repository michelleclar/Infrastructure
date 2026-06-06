package org.carl.infrastructure.workflow.definition;

public record ExecutionRecord(String nodeId, int visitNo, NodeResult result) {}
