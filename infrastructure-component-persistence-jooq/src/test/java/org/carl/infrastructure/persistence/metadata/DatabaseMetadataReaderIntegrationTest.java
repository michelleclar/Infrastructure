package org.carl.infrastructure.persistence.metadata;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseMetadataReaderIntegrationTest {
    private static final String JDBC_URL_KEY = "METADATA_TEST_JDBC_URL";
    private static final String JDBC_USER_KEY = "METADATA_TEST_JDBC_USER";
    private static final String JDBC_PASSWORD_KEY = "METADATA_TEST_JDBC_PASSWORD";

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String schemaName;
    private DatabaseMetadataReader reader;

    @BeforeAll
    void setUp() throws Exception {
        jdbcUrl = System.getenv(JDBC_URL_KEY);
        jdbcUser = System.getenv(JDBC_USER_KEY);
        jdbcPassword = System.getenv(JDBC_PASSWORD_KEY);

        assumeTrue(
                jdbcUrl != null && jdbcUser != null && jdbcPassword != null,
                () ->
                        "Set %s, %s and %s to run PostgreSQL integration tests"
                                .formatted(JDBC_URL_KEY, JDBC_USER_KEY, JDBC_PASSWORD_KEY));

        Class.forName("org.postgresql.Driver");
        schemaName = "codex_metadata_" + UUID.randomUUID().toString().replace("-", "");
        PersistenceContext persistenceContext =
                PersistenceContext.create(
                        new DriverManagerDataSource(jdbcUrl, jdbcUser, jdbcPassword));
        reader = new DatabaseMetadataReader(persistenceContext);

        execute(
                "create schema %s".formatted(schemaName),
                """
                create table %s.metadata_probe (
                    id serial primary key,
                    plain_name varchar(64) not null default 'plain',
                    "MixedName" varchar(64) not null default 'quoted',
                    status int not null,
                    tenant_id int not null,
                    constraint metadata_status_check check (status > 0)
                )
                """
                        .formatted(schemaName),
                "comment on table %s.metadata_probe is 'metadata probe table'".formatted(schemaName),
                """
                create unique index ux_metadata_probe_lower_name
                    on %s.metadata_probe ((lower(plain_name)))
                """
                        .formatted(schemaName),
                """
                create index ix_metadata_probe_plain_name
                    on %s.metadata_probe (plain_name) include (tenant_id)
                """
                        .formatted(schemaName),
                """
                create table %s."MixedTable" (
                    id int primary key
                )
                """
                        .formatted(schemaName));
    }

    @AfterAll
    void tearDown() throws Exception {
        if (jdbcUrl == null || jdbcUser == null || jdbcPassword == null || schemaName == null) {
            return;
        }

        execute("drop schema if exists %s cascade".formatted(schemaName));
    }

    @Test
    void getDbInfoIncludesSchemaOwnerAndQuotedTable() {
        DBInfo info = reader.getDBInfo();
        DBSchema schema = info.getSchema(schemaName);

        assertNotNull(schema);
        assertEquals(jdbcUser, schema.getOwner());
        assertNotNull(schema.getTable("\"MixedTable\""));
        assertNull(schema.getTable("MixedTable"));
    }

    @Test
    void getTablePreservesQuotedIdentifierSemantics() {
        DBTable table = reader.getTable(schemaName, "metadata_probe");

        assertNotNull(table);
        assertNotNull(table.getColumn("plain_name"));
        assertNotNull(table.getColumn("\"MixedName\""));
        assertNull(table.getColumn("MixedName"));
        assertNotNull(reader.getTable(schemaName, "\"MixedTable\""));
        assertNull(reader.getTable(schemaName, "MixedTable"));
    }

    @Test
    void getTableKeepsExpressionIndexUniqueAndExcludesIncludeColumnsFromKeyView() {
        DBTable table = reader.getTable(schemaName, "metadata_probe");
        DBIndex expressionIndex = findIndex(table, "ux_metadata_probe_lower_name");
        DBIndex includeIndex = findIndex(table, "ix_metadata_probe_plain_name");

        assertTrue(expressionIndex.isUnique());
        assertEquals("btree", expressionIndex.getIndexType());
        assertTrue(
                expressionIndex
                        .getDefinition()
                        .startsWith("CREATE UNIQUE INDEX ux_metadata_probe_lower_name"));
        assertEquals(1, expressionIndex.getColumns().size());
        assertTrue(expressionIndex.getColumns().getFirst().contains("lower"));

        assertFalse(includeIndex.isUnique());
        assertEquals("btree", includeIndex.getIndexType());
        assertEquals(1, includeIndex.getColumns().size());
        assertEquals("plain_name", includeIndex.getColumns().getFirst());
        assertEquals(2, includeIndex.getColumnDetails().size());
        assertTrue(includeIndex.getColumnDetails().get(1).isIncluded());
    }

    @Test
    void getTableIncludesOwnerAndCheckConstraints() {
        DBTable table = reader.getTable(schemaName, "metadata_probe");

        assertEquals(jdbcUser, table.getOwner());
        assertEquals("metadata probe table", table.getComment());
        assertEquals(1, table.getCheckConstraints().size());
        assertEquals("metadata_status_check", table.getCheckConstraints().getFirst().getName());
        assertTrue(
                table.getCheckConstraints().getFirst().getDefinition().contains("status > 0"));
    }

    private DBIndex findIndex(DBTable table, String indexName) {
        return table.getIndexList().stream()
                .filter(index -> Objects.equals(indexName, index.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Index not found: " + indexName));
    }

    private void execute(String... sqlStatements) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
                Statement statement = connection.createStatement()) {
            for (String sqlStatement : sqlStatements) {
                statement.execute(sqlStatement);
            }
        }
    }

    private record DriverManagerDataSource(String jdbcUrl, String jdbcUser, String jdbcPassword)
            implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Unsupported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {}

        @Override
        public void setLoginTimeout(int seconds) {}

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("Unsupported");
        }
    }
}
