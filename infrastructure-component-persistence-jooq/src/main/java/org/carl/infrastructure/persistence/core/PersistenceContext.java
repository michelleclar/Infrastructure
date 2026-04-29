package org.carl.infrastructure.persistence.core;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.persistence.function.ConnectionCallable;
import org.carl.infrastructure.persistence.function.ConnectionRunnable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * now look `Decorator` is don't work but if you use update delete insert ,`Decorator` can work only
 * increase update delete insert select don't change
 */
public class PersistenceContext {
    private static final ILogger logger = LoggerFactory.getLogger(PersistenceContext.class);
    private final DSLContext dsl;

    public static PersistenceContext create(DataSource dataSource) {
        DSLContext dslContext = DSL.using(dataSource, SQLDialect.POSTGRES);
        return new PersistenceContext(dslContext);
    }

    PersistenceContext(DSLContext dsl) {
        this.dsl = dsl;
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
        return dsl.connectionResult(callable::run);
    }

    public void connection(ConnectionRunnable callable) {
        dsl.connection(callable::run);
    }

    @SuppressWarnings("unchecked")
    public int execute(String sql) {
        return dsl.execute(sql);
    }

    public SQLDialect getDialect() {
        return dsl.dialect();
    }
}
