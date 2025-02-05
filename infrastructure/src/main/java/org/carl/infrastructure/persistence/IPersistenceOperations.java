package org.carl.infrastructure.persistence;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.carl.infrastructure.persistence.database.core.DSLContext;
import org.carl.infrastructure.persistence.database.metadata.DBColumn;
import org.carl.infrastructure.persistence.database.metadata.DBTable;
import org.jooq.*;

public interface IPersistenceOperations extends IPersistenceProvider {
    // NOTE: code sync by database
    default Map<String, DBColumn> getColumnMap(String schema, String tableName) {
        DBTable dbTable = new DBTable(getDSLContext()).setTableName(tableName).setSchema(schema);
        return dbTable.getColumnMap();
    }

    // NOTE: maybe code not sync by database
    default Map<String, Field<?>> getColumnMap(@Nonnull Table<?> table) {
        Field<?>[] fields = table.fields();

        Map<String, Field<?>> columnMap = new HashMap<>();
        for (Field<?> field : fields) {
            columnMap.put(field.getName(), field);
        }
        return columnMap;
    }

    default DSLContext dsl() {
        return getDSLContext();
    }

    default void run(Consumer<DSLContext> queryFunction) {
        queryFunction.accept(getDSLContext());
    }

    default <T> T get(Function<DSLContext, T> queryFunction) {
        return queryFunction.apply(getDSLContext());
    }

    default void transaction(Consumer<DSLContext> queryFunction) {
        getDSLContext().transaction(queryFunction);
    }
}
