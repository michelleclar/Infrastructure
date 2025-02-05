package org.carl.infrastructure.persistence.database.core;

import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jboss.logging.Logger;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

/**
 * now look `Decorator` is don't work
 * but if you use update delete insert ,`Decorator` can work
 * only increase update delete insert
 * select don't change
 */
public class DSLContext {
    private static final Logger log = Logger.getLogger(DSLContext.class);

    public static DSLContext create(org.jooq.DSLContext dslContext) {
        return new DSLContext(dslContext);
    }

    private final org.jooq.DSLContext dslContext;

    DSLContext(org.jooq.DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public SelectSelectStep<Record> selectPage(SelectFieldOrAsterisk... fields) {
        Field<Integer> total = DSL.count().over().as("total");
        if (fields == null || fields.length == 0) { // FIXME: check design
            fields = new SelectFieldOrAsterisk[1];
        }
        if (fields.length == 1 && fields[0] instanceof TableImpl<?> ta) {
            Field<?>[] _f = ta.fields();
            fields = Arrays.copyOf((SelectFieldOrAsterisk[]) _f, _f.length + 1);
        }
        fields[fields.length - 1] = total;
        return dslContext.select(fields);
    }

    public SelectSelectStep<Record> select(SelectFieldOrAsterisk... fields) {
        return dslContext.select(fields);
    }

    public SelectSelectStep<Record1<Integer>> selectCount() {
        return dslContext.selectCount();
    }

    public SelectSelectStep<Record> selectDistinct(SelectFieldOrAsterisk... fields) {
        return dslContext.selectDistinct(fields);
    }

    public <R extends Record> SelectWhereStep<R> selectFrom(TableLike<R> table) {
        return dslContext.selectFrom(table);
    }

    public int update(UpdatableRecord<?> record, @Nullable Condition condition) {
        // TODO: add sync es and cache,need provider sync func
        Objects.requireNonNull(condition, "condition is null");
        return dslContext.update(record.getTable()).set(record).execute();
    }

    public int insert(TableRecord<?> record) {
        // TODO: add sync es and cache,need provider sync func
        return dslContext.insertInto(record.getTable()).set(record).execute();
    }

    public int delete(TableRecord<?> record, @Nullable Condition condition) {
        Objects.requireNonNull(condition, "condition is null");
        return dslContext.delete(record.getTable()).where(condition).execute();
    }

    public int insertOnDuplicateKeyIgnore(TableRecord<?> record) {
        // TODO: add sync es and cache,need provider sync func
        return dslContext
                .insertInto(record.getTable())
                .set(record)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public int insertOnDuplicateKeyUpdate(
            TableRecord<?> record, UpdatableRecord<?> updatableRecord) {
        // TODO: add sync es and cache,need provider sync func
        return dslContext
                .insertInto(record.getTable())
                .set(record)
                .onDuplicateKeyUpdate()
                .set(updatableRecord)
                .execute();
    }

    @Deprecated
    public <T> T get(Function<DSLContext, T> queryFunction) {
        return queryFunction.apply(this);
    }

    @Deprecated
    public void run(Consumer<DSLContext> queryFunction) {
        queryFunction.accept(this);
    }

    public <T> T connectionResult(ConnectionCallable<T> callable) {
        return dslContext.connectionResult(callable);
    }

    public void connection(ConnectionRunnable runnable) {
        dslContext.connection(runnable);
    }

    public int fetchCount(Table<?> table) {
        return dslContext.fetchCount(table);
    }

    @Deprecated
    public void transaction(TransactionalRunnable transactional) {
        dslContext.transaction(transactional);
    }

    public void transaction(Consumer<DSLContext> queryFunction) {
        long start = System.currentTimeMillis();
        log.debugf("Transaction execution start time :{}", start);
        dslContext.transaction(
                trx -> {
                    DSLContext context = DSLContext.create(trx.dsl());
                    queryFunction.accept(context);
                });
        log.debugf("Transaction duration: {}", System.currentTimeMillis() - start);
    }

    // NOTE: original dslContext
    public Configuration configuration() {
        return dslContext.configuration();
    }
}
