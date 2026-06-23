package org.carl.infrastructure.mq.source;

import java.time.Instant;
import java.util.List;

public record SourceInstanceStatus(
        int instanceId,
        boolean running,
        String error,
        long numRestarts,
        long numReceivedFromSource,
        long numSystemExceptions,
        List<SourceExceptionInfo> latestSystemExceptions,
        long numSourceExceptions,
        List<SourceExceptionInfo> latestSourceExceptions,
        long numWritten,
        long lastReceivedTime,
        String workerId) {

    public SourceInstanceStatus {
        latestSystemExceptions =
                latestSystemExceptions == null ? List.of() : List.copyOf(latestSystemExceptions);
        latestSourceExceptions =
                latestSourceExceptions == null ? List.of() : List.copyOf(latestSourceExceptions);
    }

    public Instant lastReceivedAt() {
        return Instant.ofEpochMilli(lastReceivedTime);
    }
}
