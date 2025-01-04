package org.carl.persistence.engine.runtime;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import jakarta.inject.Provider;
import java.sql.Connection;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * Get database connection
 *
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public class ConnectionProvider implements Provider<Connection> {
    private static final Logger LOGGER = Logger.getLogger(ConnectionProvider.class);

    private final AgroalDataSource dataSource;

    public ConnectionProvider(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
        this.dataSource.getConfiguration().setMetricsEnabled(true);
    }

    @Override
    public Connection get() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            AgroalDataSourceMetrics metrics = dataSource.getMetrics();
            debugMetrics(metrics, e.getMessage());
            LOGGER.error("Error occurred and metrics: " + dataSource.getMetrics(), e);
            throw new RuntimeException(e);
        }
    }

    private void debugMetrics(AgroalDataSourceMetrics metrics, String prefix) {
        long maxUsedCount = metrics.maxUsedCount();
        LOGGER.tracev(
                prefix + " maxUsedCount: {0}, activeCount: {1}, availableCount: {2}",
                maxUsedCount == Long.MIN_VALUE ? -1 : maxUsedCount,
                metrics.activeCount(),
                metrics.availableCount());
    }
}
