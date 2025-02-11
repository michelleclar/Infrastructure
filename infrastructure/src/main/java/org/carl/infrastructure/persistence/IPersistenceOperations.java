package org.carl.infrastructure.persistence;

import jakarta.annotation.Nonnull;

import org.carl.infrastructure.persistence.database.core.PersistenceContext;
import org.carl.infrastructure.persistence.database.metadata.DBColumn;
import org.carl.infrastructure.persistence.database.metadata.DBTable;
import org.jooq.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// core method all is ·xxxCtx·
public interface IPersistenceOperations extends IPersistenceProvider {
    // NOTE: code sync by database
    default Map<String, DBColumn> getColumnMap(String schema, String tableName) {
        DBTable dbTable =
                new DBTable(getPersistenceContext()).setTableName(tableName).setSchema(schema);
        return dbTable.getColumnMap();
    }

    // NOTE: maybe code not sync by database

    /**
     * NOTE: this method is development platform.
     *
     * <p>now `columnMqp` is code generator info ,after change runtime table info
     */
    default Map<String, Field<?>> getColumnMap(@Nonnull Table<?> table) {
        Field<?>[] fields = table.fields();

        Map<String, Field<?>> columnMap = new HashMap<>();
        for (Field<?> field : fields) {
            columnMap.put(field.getName(), field);
        }
        return columnMap;
    }

    // core method all is ·xxxCtx·
    default PersistenceContext persistenceCtx() {
        return getPersistenceContext();
    }

    default void transaction(Consumer<PersistenceContext> queryFunction) {
        getPersistenceContext().transaction(queryFunction);
    }
}
