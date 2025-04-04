package org.carl.infrastructure.persistence.ability;

import org.carl.infrastructure.persistence.metadata.DBInfo;

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
        //        return Objects.requireNonNull(getMainTable().getPrimaryKey(), "table primary key
        // is null")
        //                .getName();
    }
}
