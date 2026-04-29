package org.carl.infrastructure.persistence;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.carl.infrastructure.persistence.metadata.DBInfo;
import org.carl.infrastructure.persistence.metadata.DBTable;
import org.carl.infrastructure.persistence.metadata.DatabaseMetadataReader;

/**
 * use database or file only provider original tool current only database and database support pg
 * <>provide database default read all schema</>
 */
public interface IPersistenceProvider {
    // persistence context,because after replace,use `Decorator`,inner use JOOQ PersistenceContext
    PersistenceContext dsl();

    void setPersistenceContext(PersistenceContext persistenceContext);

    void resetDBInfo();

    default DatabaseMetadataReader metadataReader() {
        return new DatabaseMetadataReader(dsl());
    }

    /**
     * get runtime dbinfo
     *
     * @return
     */
    default DBInfo getDBInfo() {
        return metadataReader().getDBInfo();
    }

    default DBTable getDBTable(String schema, String tableName) {
        return metadataReader().getTable(schema, tableName);
    }
}
