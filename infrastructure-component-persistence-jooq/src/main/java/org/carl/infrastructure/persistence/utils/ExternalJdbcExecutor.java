package org.carl.infrastructure.persistence.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Timeout-controlled external JDBC executor with short-lived connections.
 */
public final class ExternalJdbcExecutor implements AutoCloseable {
    private final ExternalConnectionProvider connectionProvider;
    private final ExecutorService executorService;
    private final boolean ownsExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ExternalJdbcExecutor() {
        this(new DriverManagerConnectionProvider(), newExecutorService(), true);
    }

    public ExternalJdbcExecutor(
            ExternalConnectionProvider connectionProvider, ExecutorService executorService) {
        this(connectionProvider, executorService, true);
    }

    private ExternalJdbcExecutor(
            ExternalConnectionProvider connectionProvider,
            ExecutorService executorService,
            boolean ownsExecutor) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.executorService = Objects.requireNonNull(executorService);
        this.ownsExecutor = ownsExecutor;
    }

    public ExternalDatabaseResult<ConnectionInfo> testConnection(
            ExternalDatabaseConfig config, Duration timeout) {
        return executeSql(
                config,
                timeout,
                connection -> {
                    DatabaseMetaData metaData = connection.getMetaData();
                    return new ConnectionInfo(
                            metaData.getDatabaseProductName(),
                            metaData.getDatabaseProductVersion());
                });
    }

    public <T> ExternalDatabaseResult<T> executeSql(
            ExternalDatabaseConfig config, Duration timeout, JdbcConnectionCallback<T> callback) {
        return execute(config, timeout, callback, ExternalDatabaseFailureType.SQL_EXECUTION_FAILURE);
    }

    public <T> ExternalDatabaseResult<T> executeDdl(
            ExternalDatabaseConfig config, Duration timeout, JdbcConnectionCallback<T> callback) {
        return execute(config, timeout, callback, ExternalDatabaseFailureType.SQL_EXECUTION_FAILURE);
    }

    private <T> ExternalDatabaseResult<T> execute(
            ExternalDatabaseConfig config,
            Duration timeout,
            JdbcConnectionCallback<T> callback,
            ExternalDatabaseFailureType callbackFailureType) {
        if (closed.get()) {
            return ExternalDatabaseResult.failure(
                    ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                    "ExternalJdbcExecutor is closed",
                    null);
        }
        if (callback == null) {
            return ExternalDatabaseResult.failure(
                    ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                    "JDBC callback is required",
                    null);
        }
        Duration effectiveTimeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        if (effectiveTimeout.isZero() || effectiveTimeout.isNegative()) {
            return ExternalDatabaseResult.failure(
                    ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                    "Timeout must be positive",
                    null);
        }

        AtomicReference<Connection> activeConnection = new AtomicReference<>();
        Callable<T> task = () -> runWithConnection(config, callback, callbackFailureType, activeConnection);
        Future<T> future = executorService.submit(task);
        try {
            return ExternalDatabaseResult.success(
                    future.get(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            closeQuietly(activeConnection.get());
            future.cancel(true);
            return ExternalDatabaseResult.failure(
                    ExternalDatabaseFailureType.TIMEOUT,
                    "External database operation timed out after %d ms"
                            .formatted(effectiveTimeout.toMillis()),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeQuietly(activeConnection.get());
            future.cancel(true);
            return ExternalDatabaseResult.failure(
                    ExternalDatabaseFailureType.TIMEOUT,
                    "External database operation was interrupted",
                    e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ExternalDatabaseException externalException) {
                return ExternalDatabaseResult.failure(
                        externalException.getFailureType(),
                        externalException.getMessage(),
                        externalException.getCause() == null
                                ? externalException
                                : externalException.getCause());
            }
            return ExternalDatabaseResult.failure(
                    callbackFailureType,
                    cause == null ? e.getMessage() : cause.getMessage(),
                    cause == null ? e : cause);
        }
    }

    private <T> T runWithConnection(
            ExternalDatabaseConfig config,
            JdbcConnectionCallback<T> callback,
            ExternalDatabaseFailureType callbackFailureType,
            AtomicReference<Connection> activeConnection) {
        try (Connection connection = connectionProvider.open(config)) {
            activeConnection.set(connection);
            return callback.run(connection);
        } catch (ExternalDatabaseException e) {
            throw e;
        } catch (SQLException e) {
            if (activeConnection.get() == null) {
                throw new ExternalDatabaseException(
                        ExternalDatabaseFailureType.CONNECTION_FAILURE,
                        "External database connection failed: " + e.getMessage(),
                        e);
            }
            throw new ExternalDatabaseException(
                    callbackFailureType,
                    "External database SQL execution failed: " + e.getMessage(),
                    e);
        } finally {
            activeConnection.set(null);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true) && ownsExecutor) {
            executorService.shutdownNow();
        }
    }

    private static ExecutorService newExecutorService() {
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory =
                runnable -> {
                    Thread thread =
                            new Thread(
                                    runnable,
                                    "external-jdbc-utils-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                };
        return Executors.newCachedThreadPool(threadFactory);
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Timeout cleanup should not hide the timeout failure.
        }
    }

    public record ConnectionInfo(String databaseProductName, String databaseProductVersion) {}

    @FunctionalInterface
    public interface ExternalConnectionProvider {
        Connection open(ExternalDatabaseConfig config) throws SQLException;
    }

    @FunctionalInterface
    public interface JdbcConnectionCallback<T> {
        T run(Connection connection) throws SQLException;
    }

    private static final class DriverManagerConnectionProvider implements ExternalConnectionProvider {
        @Override
        public Connection open(ExternalDatabaseConfig config) throws SQLException {
            String jdbcUrl = ExternalJdbcUrls.build(config);
            if (config.username() == null) {
                return DriverManager.getConnection(jdbcUrl);
            }
            return DriverManager.getConnection(jdbcUrl, config.username(), config.password());
        }
    }
}
