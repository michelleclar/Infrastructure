package org.carl.infrastructure.persistence.utils;

import org.carl.infrastructure.persistence.utils.ExternalJdbcExecutor.ConnectionInfo;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalJdbcExecutorTest {

    @Test
    void buildsPostgresAndMysqlUrlsWithoutRewritingOptionKeys() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("sslMode", "require");
        options.put("serverTimezone", "UTC");

        ExternalDatabaseConfig postgres =
                ExternalDatabaseConfig.builder(ExternalDatabaseKind.POSTGRESQL)
                        .host("db.example.test")
                        .databaseName("app_db")
                        .jdbcOptions(options)
                        .build();
        ExternalDatabaseConfig mysql =
                ExternalDatabaseConfig.builder(ExternalDatabaseKind.MYSQL)
                        .host("mysql.example.test")
                        .port(3307)
                        .databaseName("app_db")
                        .jdbcOptions(options)
                        .build();

        assertEquals(
                "jdbc:postgresql://db.example.test:5432/app_db?sslMode=require&serverTimezone=UTC",
                ExternalJdbcUrls.build(postgres));
        assertEquals(
                "jdbc:mysql://mysql.example.test:3307/app_db?sslMode=require&serverTimezone=UTC",
                ExternalJdbcUrls.build(mysql));
    }

    @Test
    void connectionTestReturnsProductInfoAndClosesConnection() {
        JdbcProxyFixtures.TrackingConnection tracking =
                JdbcProxyFixtures.trackingConnection("PostgreSQL", "16.2", "public");
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseResult<ConnectionInfo> result =
                    executor.testConnection(config(), Duration.ofSeconds(1));

            assertTrue(result.success());
            assertEquals("PostgreSQL", result.value().databaseProductName());
            assertEquals("16.2", result.value().databaseProductVersion());
            assertTrue(tracking.closed().get());
        }
    }

    @Test
    void ddlCallbackRunsAndClosesConnection() {
        JdbcProxyFixtures.TrackingConnection tracking =
                JdbcProxyFixtures.trackingConnection("PostgreSQL", "16.2", "public");
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseResult<Boolean> result =
                    executor.executeDdl(
                            config(),
                            Duration.ofSeconds(1),
                            connection -> {
                                try (Statement statement = connection.createStatement()) {
                                    return statement.execute("create table probe(id int)");
                                }
                            });

            assertTrue(result.success());
            assertTrue(result.value());
            assertTrue(tracking.closed().get());
        }
    }

    @Test
    void connectionFailureIsWrapped() {
        try (var executor =
                new ExternalJdbcExecutor(
                        config -> {
                            throw new SQLException("refused");
                        },
                        Executors.newSingleThreadExecutor())) {
            ExternalDatabaseResult<ConnectionInfo> result =
                    executor.testConnection(config(), Duration.ofSeconds(1));

            assertFalse(result.success());
            assertEquals(ExternalDatabaseFailureType.CONNECTION_FAILURE, result.failureType());
            assertEquals(SQLException.class.getName(), result.exceptionType());
        }
    }

    @Test
    void timeoutClosesActiveConnection() throws Exception {
        JdbcProxyFixtures.TrackingConnection tracking =
                JdbcProxyFixtures.trackingConnection("PostgreSQL", "16.2", "public");
        CountDownLatch entered = new CountDownLatch(1);
        try (var executor =
                new ExternalJdbcExecutor(config -> tracking.connection(), Executors.newSingleThreadExecutor())) {
            ExternalDatabaseResult<Boolean> result =
                    executor.executeSql(
                            config(),
                            Duration.ofMillis(50),
                            connection -> {
                                entered.countDown();
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return false;
                                }
                                return true;
                            });

            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertFalse(result.success());
            assertEquals(ExternalDatabaseFailureType.TIMEOUT, result.failureType());
            assertTrue(tracking.closed().get());
        }
    }

    private ExternalDatabaseConfig config() {
        return ExternalDatabaseConfig.builder(ExternalDatabaseKind.POSTGRESQL)
                .host("localhost")
                .databaseName("app")
                .build();
    }
}
