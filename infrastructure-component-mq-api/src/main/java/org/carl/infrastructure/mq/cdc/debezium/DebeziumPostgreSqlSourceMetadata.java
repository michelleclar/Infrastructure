package org.carl.infrastructure.mq.cdc.debezium;

public record DebeziumPostgreSqlSourceMetadata(
        String version,
        String connector,
        String name,
        DebeziumSourceTimestamp timestamp,
        Object snapshot,
        String db,
        String schema,
        String table,
        String sequence,
        Long txId,
        Long lsn,
        Long xmin)
        implements DebeziumSourceMetadata {}
