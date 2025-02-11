package org.carl.infrastructure.core.ability;

import java.util.Objects;
import org.carl.infrastructure.persistence.database.metadata.DBInfo;
import org.jooq.Schema;
import org.jooq.Table;

/** runtime get metadata */
public interface IMetadataAbility extends IPersistenceAbility {
    Table<?> getMainTable();

    default Schema getSchema() {
        return getMainTable().getSchema();
    }

    default DBInfo getDBInfo() {
        return getPersistenceOperations().getDBInfo();
    }

    default String getPK() {
        return Objects.requireNonNull(getMainTable().getPrimaryKey(), "table primary key is null")
                .getName();
    }
}
