package org.carl.infrastructure.persistence.core;

import io.agroal.api.AgroalDataSource;

import org.carl.infrastructure.persistence.function.ConnectionCallable;
import org.carl.infrastructure.persistence.function.ConnectionRunnable;
import org.carl.infrastructure.persistence.sql.SqlLoggerFactory;
import org.jboss.logging.Logger;
import org.jdbi.v3.core.Jdbi;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * now look `Decorator` is don't work
 * but if you use update delete insert ,`Decorator` can work
 * only increase update delete insert
 * select don't change
 */
public class PersistenceContext {
    private static final Logger log = Logger.getLogger(PersistenceContext.class);
    private final Jdbi jdbi;

    public static PersistenceContext create(AgroalDataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.setSqlLogger(SqlLoggerFactory.DEFAULT);
        return new PersistenceContext(jdbi);
    }

    PersistenceContext(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Deprecated
    public <T> T get(Function<PersistenceContext, T> queryFunction) {
        return queryFunction.apply(this);
    }

    @Deprecated
    public void run(Consumer<PersistenceContext> queryFunction) {
        queryFunction.accept(this);
    }

    public <T> T connectionResult(ConnectionCallable<T> callable) {
        try {
            return jdbi.withHandle(
                    handle -> {
                        Connection connection = handle.getConnection();
                        try {
                            return callable.run(connection);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void connection(ConnectionRunnable callable) {
        try {
            jdbi.useHandle(
                    handle -> {
                        Connection connection = handle.getConnection();
                        try {
                            callable.run(connection);
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public int execute(String sql) {
        return this.jdbi.withHandle(h -> h.execute(sql));
    }
}
