package org.carl.infrastructure.ability;

import jakarta.transaction.Transactional;
import org.carl.infrastructure.persistence.IPersistenceOperations;

public interface ITableCreateAbility extends IMetadataAbility {

    //    void fitOut(TableWrapper wrapper);

    default void onTableCreated(boolean isFirst) {}

    @Transactional
    default void create(IPersistenceOperations db) {
        String mainTable = getMainTable().getName();
        if (!mainTable.toLowerCase().equals(mainTable)) {
            throw new RuntimeException("%s need Up Case table name".formatted(mainTable));
        }

        // TODO: default add archive time column

        // TODO: interface to gen column
        //        TableWrapper wrapper = TableWrapper.withName(mainTable)
        //            .setSchema(getSchemaName());
        //
        //        fitOut(wrapper);
        //
        //        if (this instanceof ITreeAbility ability) {
        //            wrapper.addColumn(ability.getParentKeyColumn());
        //        }
        //        if (this instanceof ISortAbility ability) {
        //            wrapper.addColumn(ability.getSortColumn().getColumn());
        //        }
        //        if (this instanceof ISecurityAbility ability) {
        //            ability.getSignColumns().forEach(wrapper::addColumn);
        //        }
        //
        //        if (this instanceof ISoftDeleteAbility ability) {
        //            if (this instanceof IArchiveWhenDelete) {
        //                throw new RuntimeException("");
        //            }
        //
        //            wrapper.addColumn(ability.getSoftDeleteColumn());
        //            wrapper.addColumn("t_delete");
        //            wrapper.addColumn("id_at_auth_user__delete");
        //        }
        //
        //        boolean build = new TableBuilder(db).build(wrapper);
        //
        //        if (this instanceof IArchiveWhenDelete ability) {
        //            wrapper.setName(ability.getArchiveTableName());
        //            wrapper.addColumn("t_archive", "archive time", "now()");
        //            wrapper.addColumn("id_at_auth_user__archive", "archive member");
        //            new TableBuilder(db).build(wrapper);
        //        }

        //        this.onTableCreated(build);
    }
}
