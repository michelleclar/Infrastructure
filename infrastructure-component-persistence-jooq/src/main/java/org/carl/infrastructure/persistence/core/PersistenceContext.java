package org.carl.infrastructure.persistence.core;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.persistence.engine.runtime.DslContextFactory;
import org.carl.infrastructure.persistence.function.ConnectionCallable;
import org.carl.infrastructure.persistence.function.ConnectionRunnable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.ResultQuery;

import javax.sql.DataSource;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
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
        DSLContext dslContext = DslContextFactory.create(dataSource);
        return new PersistenceContext(dslContext);
    }

    PersistenceContext(DSLContext dsl) {
        this.dsl = dsl;
    }

    public <T> T get(Function<DSLContext, T> queryFunction) {
        return queryFunction.apply(this.dsl);
    }

    public void run(Consumer<DSLContext> queryFunction) {
        queryFunction.accept(this.dsl);
    }

    public <T> T connectionResult(ConnectionCallable<T> callable) {
        return dsl.connectionResult(callable::run);
    }

    public void connection(ConnectionRunnable callable) {
        dsl.connection(callable::run);
    }

    public SQLDialect getDialect() {
        return dsl.dialect();
    }

    public <R extends Record> CompletionStage<Result<R>> fetchAsync(
            Function<DSLContext, ? extends ResultQuery<R>> queryFunction) {
        return queryFunction.apply(this.dsl).fetchAsync();
    }

    public <R extends Record> CompletionStage<Result<R>> fetchAsync(
            Function<DSLContext, ? extends ResultQuery<R>> queryFunction, Executor executor) {
        return queryFunction.apply(this.dsl).fetchAsync(executor);
    }

    public <R extends Record, T> CompletionStage<T> fetchAsync(
            Function<DSLContext, ? extends ResultQuery<R>> queryFunction,
            Function<Result<R>, T> mapper) {
        return queryFunction.apply(this.dsl).fetchAsync().thenApply(mapper);
    }

    public <R extends Record, T> CompletionStage<T> fetchAsync(
            Function<DSLContext, ? extends ResultQuery<R>> queryFunction,
            Function<Result<R>, T> mapper, Executor executor) {
        return queryFunction.apply(this.dsl).fetchAsync(executor).thenApply(mapper);
    }

    public CompletionStage<Integer> executeAsync(
            Function<DSLContext, ? extends Query> queryFunction) {
        return queryFunction.apply(this.dsl).executeAsync();
    }

    public CompletionStage<Integer> executeAsync(
            Function<DSLContext, ? extends Query> queryFunction, Executor executor) {
        return queryFunction.apply(this.dsl).executeAsync(executor);
    }
}
