package org.carl.infrastructure.ability;

import java.util.Objects;
import org.carl.infrastructure.persistence.metadata.DBInfo;
import org.jooq.Schema;
import org.jooq.Table;

/** runtime get metadata */
public interface IMetadataAbility extends IPersistenceAbility {
    String getMainTable();

    default String getSchema() {
        return "public";
    }

    default DBInfo getDBInfo() {
        return getPersistenceOperations().getDBInfo();
    }

    default String getPK() {
        return "id";
//        return Objects.requireNonNull(getMainTable().getPrimaryKey(), "table primary key is null")
//                .getName();
    }
}
