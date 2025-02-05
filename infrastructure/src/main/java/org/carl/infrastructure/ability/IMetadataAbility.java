package org.carl.infrastructure.ability;

import org.jooq.Schema;
import org.jooq.Table;

import java.util.Objects;

public interface IMetadataAbility extends IPersistenceAbility{
    Table<?> getMainTable();

    default Schema getSchema() {
        return getMainTable().getSchema();
    }

    default String getPK() {
        return Objects.requireNonNull(getMainTable().getPrimaryKey(),"table primary key is null").getName();
    }
}
