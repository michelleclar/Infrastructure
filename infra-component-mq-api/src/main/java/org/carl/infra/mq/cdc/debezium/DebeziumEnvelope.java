package org.carl.infra.mq.cdc.debezium;

public record DebeziumEnvelope(
        DebeziumOperation op, DebeziumSourceMetadata source, DebeziumSourceTimestamp timestamp) {}
