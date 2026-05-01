package org.carl.infrastructure.persistence;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.carl.infrastructure.persistence.metadata.DBColumn;

import java.util.Map;

// core method all is ·xxxCtx·
public interface IPersistenceOperations extends IPersistenceProvider {
    // NOTE: code sync by database
    default Map<String, DBColumn> getColumnMap(String schema, String tableName) {
        return metadataReader().getColumnMap(schema, tableName);
    }

    // NOTE: maybe code not sync by database

    // core method all is ·xxxCtx·
    default PersistenceContext persistenceCtx() {
        return dsl();
    }

    default void execute(String sql) {
        dsl().run(dsl -> dsl.execute(sql));
    }
}
