package org.carl.infra.mq.source;

import java.time.Instant;

public record SourceExceptionInfo(String exceptionString, long timestampMs) {
    public Instant timestamp() {
        return Instant.ofEpochMilli(timestampMs);
    }
}
