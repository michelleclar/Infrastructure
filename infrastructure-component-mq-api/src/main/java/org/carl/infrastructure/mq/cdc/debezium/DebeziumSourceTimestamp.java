package org.carl.infrastructure.mq.cdc.debezium;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public record DebeziumSourceTimestamp(Long ts_ms, Long ts_us, Long ts_ns) {
    public Optional<Instant> instant() {
        if (ts_ns != null) {
            return Optional.of(Instant.EPOCH.plusNanos(ts_ns));
        }
        if (ts_us != null) {
            return Optional.of(Instant.EPOCH.plus(ts_us, ChronoUnit.MICROS));
        }
        if (ts_ms != null) {
            return Optional.of(Instant.ofEpochMilli(ts_ms));
        }
        return Optional.empty();
    }
}
