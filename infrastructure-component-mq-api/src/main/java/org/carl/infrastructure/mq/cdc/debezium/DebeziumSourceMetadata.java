package org.carl.infrastructure.mq.cdc.debezium;

public sealed interface DebeziumSourceMetadata
        permits DebeziumPostgreSqlSourceMetadata, DebeziumMySqlSourceMetadata {
    String version();

    String connector();

    String name();

    DebeziumSourceTimestamp timestamp();

    Object snapshot();

    String db();

    String table();
}
