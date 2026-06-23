package org.carl.infrastructure.persistence.utils;

import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.DatabaseInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.TableInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDatabaseMetadataReaderTest {

    @Test
    void readsDatabaseSchemaTableColumnsKeysIndexesAndTypes() {
        JdbcProxyFixtures.TrackingConnection tracking = JdbcProxyFixtures.metadataConnection();
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseMetadataReader reader = new ExternalDatabaseMetadataReader(executor);

            ExternalDatabaseResult<DatabaseInfo> result =
                    reader.readDatabase(config(), Duration.ofSeconds(1));

            assertTrue(result.success());
            DatabaseInfo database = result.value();
            assertEquals("PostgreSQL", database.databaseProductName());
            assertEquals("16.2", database.databaseProductVersion());
            assertEquals("CamelSchema", database.defaultSchema());
            assertEquals("CamelSchema", database.schemas().getFirst().schemaName());
            assertEquals("OrderLine", database.schemas().getFirst().tables().getFirst().tableName());
            assertTrue(tracking.closed().get());
        }
    }

    @Test
    void tableDetailPreservesExactNamesFromJdbcMetadata() {
        JdbcProxyFixtures.TrackingConnection tracking = JdbcProxyFixtures.metadataConnection();
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseMetadataReader reader = new ExternalDatabaseMetadataReader(executor);

            ExternalDatabaseResult<TableInfo> result =
                    reader.readTable(config(), Duration.ofSeconds(1), "CamelSchema", "OrderLine");

            assertTrue(result.success());
            TableInfo table = result.value();
            assertNotNull(table);
            assertEquals("OrderLine", table.tableName());
            assertEquals(List.of("ID"), table.primaryKey().columnNames());
            assertEquals("PK_OrderLine", table.primaryKey().name());
            assertEquals("ID", table.columns().get(0).name());
            assertEquals("UserID", table.columns().get(1).name());
            assertEquals("int4", table.columns().get(1).type().rawTypeName());
            assertEquals(DatabaseMetadataSnapshot.ColumnTypeFamily.INTEGER, table.columns().get(1).type().family());
            assertEquals("FK_OrderLine_User", table.foreignKeys().getFirst().name());
            assertEquals(List.of("UserID"), table.foreignKeys().getFirst().columnNames());
            assertEquals("User", table.foreignKeys().getFirst().referencedTableName());
            assertEquals("IX_OrderLine_UserID", table.indexes().getFirst().name());
            assertFalse(table.indexes().getFirst().unique());
        }
    }

    @Test
    void tableLookupDoesNotFoldCallerInput() {
        JdbcProxyFixtures.TrackingConnection tracking = JdbcProxyFixtures.metadataConnection();
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseMetadataReader reader = new ExternalDatabaseMetadataReader(executor);

            ExternalDatabaseResult<TableInfo> result =
                    reader.readTable(config(), Duration.ofSeconds(1), "CamelSchema", "orderline");

            assertTrue(result.success());
            assertNull(result.value());
        }
    }

    @Test
    void mysqlCatalogShapeIsPreservedWhenSchemasAreEmpty() {
        JdbcProxyFixtures.TrackingConnection tracking = JdbcProxyFixtures.mysqlMetadataConnection();
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseMetadataReader reader = new ExternalDatabaseMetadataReader(executor);

            ExternalDatabaseResult<DatabaseInfo> database =
                    reader.readDatabase(mysqlConfig(), Duration.ofSeconds(1));
            ExternalDatabaseResult<TableInfo> table =
                    reader.readTable(mysqlConfig(), Duration.ofSeconds(1), "AppCatalog", "OrderLine");

            assertTrue(database.success());
            assertEquals("MySQL", database.value().databaseProductName());
            assertEquals("AppCatalog", database.value().schemas().getFirst().catalogName());
            assertEquals("AppCatalog", database.value().schemas().getFirst().schemaName());
            assertEquals("OrderLine", database.value().schemas().getFirst().tables().getFirst().tableName());
            assertTrue(table.success());
            assertEquals("AppCatalog", table.value().catalogName());
            assertEquals("PRIMARY", table.value().primaryKey().name());
            assertEquals(List.of("ID"), table.value().primaryKey().columnNames());
        }
    }

    private ExternalDatabaseConfig config() {
        return ExternalDatabaseConfig.builder(ExternalDatabaseKind.POSTGRESQL)
                .host("localhost")
                .databaseName("app")
                .build();
    }

    private ExternalDatabaseConfig mysqlConfig() {
        return ExternalDatabaseConfig.builder(ExternalDatabaseKind.MYSQL)
                .host("localhost")
                .databaseName("app")
                .build();
    }
}
