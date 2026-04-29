package org.carl.infrastructure.persistence.engine.runtime;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Get database connection
 *
 * @author <a href="mailto:leo.tu.taipei@gmail.com">Leo Tu</a>
 */
public class ConnectionProvider implements Supplier<Connection> {
    private static final ILogger logger = LoggerFactory.getLogger(ConnectionProvider.class);

    private final DataSource dataSource;

    public ConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection get() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Error occurred while getting database connection", e);
            throw new RuntimeException(e);
        }
    }
}
