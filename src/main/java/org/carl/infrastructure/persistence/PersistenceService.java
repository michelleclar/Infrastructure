package org.carl.infrastructure.persistence;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.carl.infrastructure.persistence.engine.core.DSLContextX;
import org.jboss.logging.Logger;
import org.jooq.Configuration;
import org.jooq.impl.TableImpl;

public class PersistenceService {
    @Inject DSLContextX dsl;

    private static final Logger log = Logger.getLogger(PersistenceService.class);

    public <T> T get(Function<DSLContextX, T> queryFunction) {
        return queryFunction.apply(dsl);
    }

    public <T, R extends Record & org.jooq.Record> T limit(
            Function<DSLContextX, T> queryFunction, TableImpl<R> table) {
        return queryFunction.apply(dsl);
    }

    <T extends Record & org.jooq.Record> Integer count(TableImpl<T> table) {
        return this.get(dsl -> dsl.fetchCount(table));
    }

    public void run(Consumer<DSLContextX> queryFunction) {
        queryFunction.accept(dsl);
    }

    static Map<String, Object> dtoFactory = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T dto(Function<Configuration, T> function) {
        Configuration conf = dsl.configuration();
        String key = function.getClass().getSimpleName().split("[$]")[0];
        Object o = dtoFactory.get(key);
        if (o == null) {
            dtoFactory.put(key, function.apply(conf));
        }

        return (T) dtoFactory.get(key);
    }

    public void transaction(Consumer<DSLContextX> queryFunction) {
        long start = System.currentTimeMillis();
        log.debugv("Transaction execution start time :{0}", start);
        dsl.transaction(
                configuration -> {
                    DSLContextX dsl = DSLContextX.create(configuration);
                    queryFunction.accept(dsl);
                });
        log.debugv("Transaction duration: {}", System.currentTimeMillis() - start);
    }
}
