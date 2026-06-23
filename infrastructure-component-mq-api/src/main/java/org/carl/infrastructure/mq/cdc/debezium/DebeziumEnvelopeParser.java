package org.carl.infrastructure.mq.cdc.debezium;

import java.util.Map;

public final class DebeziumEnvelopeParser {

    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_OP = "op";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_CONNECTOR = "connector";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TS_MS = "ts_ms";
    private static final String FIELD_TS_US = "ts_us";
    private static final String FIELD_TS_NS = "ts_ns";
    private static final String FIELD_SNAPSHOT = "snapshot";
    private static final String FIELD_DB = "db";
    private static final String FIELD_SCHEMA = "schema";
    private static final String FIELD_TABLE = "table";
    private static final String FIELD_SEQUENCE = "sequence";
    private static final String FIELD_TX_ID = "txId";
    private static final String FIELD_LSN = "lsn";
    private static final String FIELD_XMIN = "xmin";
    private static final String FIELD_SERVER_ID = "server_id";
    private static final String FIELD_GTID = "gtid";
    private static final String FIELD_FILE = "file";
    private static final String FIELD_POS = "pos";
    private static final String FIELD_ROW = "row";
    private static final String FIELD_THREAD = "thread";
    private static final String FIELD_QUERY = "query";

    public DebeziumEnvelope parseRecord(Map<String, ?> record) {
        return parsePayload(mapField(record, "record", FIELD_PAYLOAD));
    }

    public DebeziumEnvelope parsePayload(Map<String, ?> payload) {
        String op = requiredStringField(payload, "payload", FIELD_OP);
        Map<String, ?> source = mapField(payload, "payload", FIELD_SOURCE);
        return new DebeziumEnvelope(
                DebeziumOperation.fromCode(op), parseSource(source), timestamp(payload, "payload"));
    }

    public DebeziumSourceMetadata parseSource(Map<String, ?> source) {
        String connector = requiredStringField(source, "payload.source", FIELD_CONNECTOR);
        return switch (connector) {
            case "postgresql" -> parsePostgreSqlSource(source);
            case "mysql" -> parseMySqlSource(source);
            default -> throw new IllegalArgumentException(
                    "Unsupported Debezium connector at payload.source.connector: " + connector);
        };
    }

    private static DebeziumPostgreSqlSourceMetadata parsePostgreSqlSource(Map<String, ?> source) {
        return new DebeziumPostgreSqlSourceMetadata(
                stringField(source, "payload.source", FIELD_VERSION),
                requiredStringField(source, "payload.source", FIELD_CONNECTOR),
                stringField(source, "payload.source", FIELD_NAME),
                timestamp(source, "payload.source"),
                source.get(FIELD_SNAPSHOT),
                stringField(source, "payload.source", FIELD_DB),
                stringField(source, "payload.source", FIELD_SCHEMA),
                stringField(source, "payload.source", FIELD_TABLE),
                stringField(source, "payload.source", FIELD_SEQUENCE),
                longField(source, "payload.source", FIELD_TX_ID),
                longField(source, "payload.source", FIELD_LSN),
                longField(source, "payload.source", FIELD_XMIN));
    }

    private static DebeziumMySqlSourceMetadata parseMySqlSource(Map<String, ?> source) {
        return new DebeziumMySqlSourceMetadata(
                stringField(source, "payload.source", FIELD_VERSION),
                requiredStringField(source, "payload.source", FIELD_CONNECTOR),
                stringField(source, "payload.source", FIELD_NAME),
                timestamp(source, "payload.source"),
                source.get(FIELD_SNAPSHOT),
                stringField(source, "payload.source", FIELD_DB),
                stringField(source, "payload.source", FIELD_TABLE),
                longField(source, "payload.source", FIELD_SERVER_ID),
                stringField(source, "payload.source", FIELD_GTID),
                stringField(source, "payload.source", FIELD_FILE),
                longField(source, "payload.source", FIELD_POS),
                longField(source, "payload.source", FIELD_ROW),
                longField(source, "payload.source", FIELD_THREAD),
                stringField(source, "payload.source", FIELD_QUERY));
    }

    private static DebeziumSourceTimestamp timestamp(Map<String, ?> map, String path) {
        return new DebeziumSourceTimestamp(
                longField(map, path, FIELD_TS_MS),
                longField(map, path, FIELD_TS_US),
                longField(map, path, FIELD_TS_NS));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> mapField(Map<String, ?> map, String path, String field) {
        Object value = map.get(field);
        if (value instanceof Map<?, ?> valueMap) {
            return (Map<String, ?>) valueMap;
        }
        throw new IllegalArgumentException("Expected object at " + path + "." + field);
    }

    private static String requiredStringField(Map<String, ?> map, String path, String field) {
        String value = stringField(map, path, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing string at " + path + "." + field);
        }
        return value;
    }

    private static String stringField(Map<String, ?> map, String path, String field) {
        Object value = map.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalArgumentException("Expected string at " + path + "." + field);
    }

    private static Long longField(Map<String, ?> map, String path, String field) {
        Object value = map.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new IllegalArgumentException("Expected number at " + path + "." + field);
    }
}
