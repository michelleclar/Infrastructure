package org.carl.infrastructure.mq.cdc.debezium;

public record DebeziumEnvelope(
        DebeziumOperation op, DebeziumSourceMetadata source, DebeziumSourceTimestamp timestamp) {}
