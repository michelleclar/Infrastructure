package org.carl.infrastructure.mq.cdc.debezium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

class DebeziumEnvelopeParserTest {

    private final DebeziumEnvelopeParser parser = new DebeziumEnvelopeParser();

    @Test
    void parseRecordReadsPostgreSqlSourceMetadataFromPayloadSource() {
        Map<String, Object> source =
                map(
                        "version", "3.5.2.Final",
                        "connector", "postgresql",
                        "name", "PostgreSQL_server",
                        "ts_ms", 1559033904863L,
                        "ts_us", 1559033904863123L,
                        "ts_ns", 1559033904863123456L,
                        "snapshot", false,
                        "db", "inventory",
                        "schema", "public",
                        "table", "customers",
                        "sequence", "[\"46523120\",\"46523128\"]",
                        "txId", 556L,
                        "lsn", 46523128L,
                        "xmin", 123L);
        Map<String, Object> payload =
                map(
                        "source", source,
                        "op", "c",
                        "ts_ms", 1559033904961L,
                        "ts_us", 1559033904961654L,
                        "ts_ns", 1559033904961654789L);

        DebeziumEnvelope envelope = parser.parseRecord(Map.of("payload", payload));

        assertEquals(DebeziumOperation.CREATE, envelope.op());
        DebeziumPostgreSqlSourceMetadata metadata =
                assertInstanceOf(DebeziumPostgreSqlSourceMetadata.class, envelope.source());
        assertEquals("3.5.2.Final", metadata.version());
        assertEquals("postgresql", metadata.connector());
        assertEquals("PostgreSQL_server", metadata.name());
        assertEquals(false, metadata.snapshot());
        assertEquals("inventory", metadata.db());
        assertEquals("public", metadata.schema());
        assertEquals("customers", metadata.table());
        assertEquals("[\"46523120\",\"46523128\"]", metadata.sequence());
        assertEquals(556L, metadata.txId());
        assertEquals(46523128L, metadata.lsn());
        assertEquals(123L, metadata.xmin());
        assertEquals(1559033904863L, metadata.timestamp().ts_ms());
        assertEquals(1559033904863123L, metadata.timestamp().ts_us());
        assertEquals(1559033904863123456L, metadata.timestamp().ts_ns());
        assertEquals(
                Instant.EPOCH.plusNanos(1559033904863123456L),
                metadata.timestamp().instant().orElseThrow());
    }

    @Test
    void parsePayloadReadsMySqlSourceMetadataFromSource() {
        Map<String, Object> source =
                map(
                        "version", "3.5.2.Final",
                        "connector", "mysql",
                        "name", "mysql-server-1",
                        "ts_ms", 1465581029100L,
                        "ts_us", 1465581029100000L,
                        "ts_ns", 1465581029100000000L,
                        "snapshot", false,
                        "db", "inventory",
                        "table", "customers",
                        "server_id", 223344L,
                        "gtid", null,
                        "file", "mysql-bin.000003",
                        "pos", 484L,
                        "row", 0L,
                        "thread", 7L,
                        "query", "UPDATE customers SET first_name='Anne Marie' WHERE id=1004");
        Map<String, Object> payload =
                map(
                        "source", source,
                        "op", "u",
                        "ts_ms", 1465581029523L,
                        "ts_us", 1465581029523758L,
                        "ts_ns", 1465581029523758914L);

        DebeziumEnvelope envelope = parser.parsePayload(payload);

        assertEquals(DebeziumOperation.UPDATE, envelope.op());
        DebeziumMySqlSourceMetadata metadata =
                assertInstanceOf(DebeziumMySqlSourceMetadata.class, envelope.source());
        assertEquals("3.5.2.Final", metadata.version());
        assertEquals("mysql", metadata.connector());
        assertEquals("mysql-server-1", metadata.name());
        assertEquals(false, metadata.snapshot());
        assertEquals("inventory", metadata.db());
        assertEquals("customers", metadata.table());
        assertEquals(223344L, metadata.serverId());
        assertEquals(null, metadata.gtid());
        assertEquals("mysql-bin.000003", metadata.file());
        assertEquals(484L, metadata.pos());
        assertEquals(0L, metadata.row());
        assertEquals(7L, metadata.thread());
        assertEquals("UPDATE customers SET first_name='Anne Marie' WHERE id=1004", metadata.query());
        assertEquals(
                Instant.EPOCH.plusNanos(1465581029100000000L),
                metadata.timestamp().instant().orElseThrow());
    }

    @Test
    void operationMappingUsesDebeziumOpCodes() {
        assertEquals(DebeziumOperation.CREATE, DebeziumOperation.fromCode("c"));
        assertEquals(DebeziumOperation.UPDATE, DebeziumOperation.fromCode("u"));
        assertEquals(DebeziumOperation.DELETE, DebeziumOperation.fromCode("d"));
        assertEquals(DebeziumOperation.READ, DebeziumOperation.fromCode("r"));
        assertEquals(DebeziumOperation.TRUNCATE, DebeziumOperation.fromCode("t"));
        assertEquals(DebeziumOperation.MESSAGE, DebeziumOperation.fromCode("m"));
    }

    @Test
    void timestampParsingFallsBackFromNanosecondsToMicrosecondsToMilliseconds() {
        assertEquals(
                Instant.EPOCH.plusNanos(1_234_567L),
                new DebeziumSourceTimestamp(1L, 1_234L, 1_234_567L).instant().orElseThrow());
        assertEquals(
                Instant.EPOCH.plus(1_234L, java.time.temporal.ChronoUnit.MICROS),
                new DebeziumSourceTimestamp(1L, 1_234L, null).instant().orElseThrow());
        assertEquals(Instant.ofEpochMilli(1L), new DebeziumSourceTimestamp(1L, null, null).instant().orElseThrow());
        assertTrue(new DebeziumSourceTimestamp(null, null, null).instant().isEmpty());
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put((String) values[i], values[i + 1]);
        }
        return map;
    }
}
