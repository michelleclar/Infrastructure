package org.carl.infrastructure.mq.source;

import java.time.Instant;

public record SourceExceptionInfo(String exceptionString, long timestampMs) {
    public Instant timestamp() {
        return Instant.ofEpochMilli(timestampMs);
    }
}
