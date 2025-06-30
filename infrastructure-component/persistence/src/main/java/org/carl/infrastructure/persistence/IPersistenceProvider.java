package org.carl.infrastructure.persistence;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.carl.infrastructure.persistence.metadata.DBInfo;
import org.carl.infrastructure.persistence.metadata.DBSchema;
import org.carl.infrastructure.persistence.metadata.DBTable;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * use database or file only provider original tool current only database and database support pg
 * <>provide database default read all schema</>
 */
public interface IPersistenceProvider {
    // persistence context,because after replace,use `Decorator`,inner use JOOQ PersistenceContext
    PersistenceContext dsl();

    void setPersistenceContext(PersistenceContext persistenceContext);

    void resetDBInfo();

    /**
     * get runtime dbinfo
     *
     * @return
     */
    default DBInfo getDBInfo() {
        return dsl().connectionResult(
                        r -> {
                            try {
                                DatabaseMetaData metaData = r.getMetaData();

                                DBInfo info = new DBInfo(metaData.getDatabaseProductName());

                                try (ResultSet schemasRs = metaData.getSchemas()) {
                                    while (schemasRs.next()) {
                                        info.addSchema(
                                                new DBSchema(schemasRs.getString("TABLE_SCHEM")));
                                    }
                                }

                                try (ResultSet tablesRs =
                                        metaData.getTables(
                                                null, null, null, new String[] {"TABLE"})) {
                                    while (tablesRs.next()) {
                                        String tableName = tablesRs.getString("TABLE_NAME");
                                        String schema = tablesRs.getString("TABLE_SCHEM");
                                        DBTable table =
                                                new DBTable(dsl())
                                                        .setTableName(tableName)
                                                        .setSchema(schema);
                                        info.getSchema(schema).addTable(table);
                                    }
                                }

                                return info;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
    }
}
