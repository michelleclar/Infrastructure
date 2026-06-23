package org.carl.infrastructure.mq.cdc.debezium;

public record DebeziumMySqlSourceMetadata(
        String version,
        String connector,
        String name,
        DebeziumSourceTimestamp timestamp,
        Object snapshot,
        String db,
        String table,
        Long serverId,
        String gtid,
        String file,
        Long pos,
        Long row,
        Long thread,
        String query)
        implements DebeziumSourceMetadata {}
