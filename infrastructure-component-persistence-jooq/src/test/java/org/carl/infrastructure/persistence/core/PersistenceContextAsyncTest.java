package org.carl.infrastructure.persistence.core;

import org.jooq.Record;
import org.jooq.Result;
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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistenceContextAsyncTest {

    private static final String JDBC_URL_KEY = "PERSISTENCE_TEST_JDBC_URL";
    private static final String JDBC_USER_KEY = "PERSISTENCE_TEST_JDBC_USER";
    private static final String JDBC_PASSWORD_KEY = "PERSISTENCE_TEST_JDBC_PASSWORD";

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String schemaName;
    private PersistenceContext context;

    @BeforeAll
    void setUp() throws Exception {
        jdbcUrl = System.getenv(JDBC_URL_KEY);
        jdbcUser = System.getenv(JDBC_USER_KEY);
        jdbcPassword = System.getenv(JDBC_PASSWORD_KEY);

        assumeTrue(
                jdbcUrl != null && jdbcUser != null && jdbcPassword != null,
                () -> "Set %s, %s and %s to run async integration tests"
                        .formatted(JDBC_URL_KEY, JDBC_USER_KEY, JDBC_PASSWORD_KEY));

        Class.forName("org.postgresql.Driver");
        schemaName = "async_test_" + UUID.randomUUID().toString().replace("-", "_");
        context = PersistenceContext.create(new DriverManagerDataSource(jdbcUrl, jdbcUser, jdbcPassword));

        execute(
                "create schema %s".formatted(schemaName),
                """
                create table %s.book (
                    id serial primary key,
                    title varchar(256) not null,
                    author varchar(256) not null
                )
                """.formatted(schemaName));
    }

    @AfterAll
    void tearDown() throws Exception {
        if (jdbcUrl == null) return;
        execute("drop schema if exists %s cascade".formatted(schemaName));
    }

    @Test
    void fetchAsyncReturnsQueryResults() throws Exception {
        execute("insert into %s.book (title, author) values ('Test Book', 'Test Author')".formatted(schemaName));

        Result<Record> result = context.fetchAsync(dsl ->
                        dsl.resultQuery("select * from %s.book".formatted(schemaName)))
                .toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    void fetchAsyncWithMapperTransformsResult() throws Exception {
        execute("insert into %s.book (title, author) values ('Mapper Book', 'Mapper Author')".formatted(schemaName));

        int count = context.<Record, Integer>fetchAsync(
                        dsl -> dsl.resultQuery("select count(*) as cnt from %s.book where title = 'Mapper Book'".formatted(schemaName)),
                        result -> result.get(0).get("cnt", Integer.class))
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertEquals(1, count);
    }

    @Test
    void fetchAsyncWithCustomExecutor() throws Exception {
        String threadName = context.fetchAsync(
                        dsl -> dsl.resultQuery("select * from %s.book".formatted(schemaName)),
                        Executors.newSingleThreadExecutor(r -> new Thread(r, "custom-exec")))
                .thenApply(result -> Thread.currentThread().getName())
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertTrue(threadName.startsWith("custom-exec"));
    }

    @Test
    void fetchAsyncWithMapperAndCustomExecutor() throws Exception {
        execute("insert into %s.book (title, author) values ('Exec Book', 'Exec Author')".formatted(schemaName));

        int count = context.<Record, Integer>fetchAsync(
                        dsl -> dsl.resultQuery("select count(*) as cnt from %s.book where title = 'Exec Book'".formatted(schemaName)),
                        result -> result.get(0).get("cnt", Integer.class),
                        Executors.newCachedThreadPool())
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertEquals(1, count);
    }

    @Test
    void executeAsyncInsertsRows() throws Exception {
        int rows = context.executeAsync(dsl ->
                        dsl.query("insert into %s.book (title, author) values ('Async Book', 'Async Author')".formatted(schemaName)))
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertEquals(1, rows);
    }

    @Test
    void executeAsyncWithCustomExecutor() throws Exception {
        String threadName = context.executeAsync(
                        dsl -> dsl.query("insert into %s.book (title, author) values ('ExecAsync Book', 'ExecAsync Author')".formatted(schemaName)),
                        Executors.newSingleThreadExecutor(r -> new Thread(r, "dml-exec")))
                .thenApply(rows -> Thread.currentThread().getName())
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);

        assertTrue(threadName.startsWith("dml-exec"));
    }

    private void execute(String... sqlStatements) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             Statement statement = connection.createStatement()) {
            for (String sql : sqlStatements) {
                statement.execute(sql);
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
