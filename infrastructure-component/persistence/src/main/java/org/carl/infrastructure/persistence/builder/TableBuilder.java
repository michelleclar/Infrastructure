package org.carl.infrastructure.persistence.builder;

import org.carl.infrastructure.persistence.IPersistenceOperations;
import org.carl.infrastructure.persistence.metadata.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Deprecated
public class TableBuilder {

    DBInfo info;
    IPersistenceOperations db;

    public TableBuilder(IPersistenceOperations db) {
        this.db = db;
        this.info = db.getDBInfo();
    }

    public boolean build(TableWrapper wrapper) {
        var result = false;
        String schema = wrapper.getSchema();
        String name = wrapper.getTableName();

        if (info.getSchema(schema) == null) {
            db.execute("create schema if not exists " + wrapper.getSchema());
            info.addSchema(new DBSchema(wrapper.getSchema()));
        }

        List<TableBase> inherits = wrapper.getInherits();

        if (inherits != null && !inherits.isEmpty()) {
            inherits.forEach(
                    inherit -> {
                        if (!info.getSchema(inherit.getSchema())
                                .containsTable(inherit.getTableName())) {
                            throw new RuntimeException("Table " + inherit + " does not exist");
                        }
                    });
        }

        if (!info.getSchema(wrapper.getSchema()).containsTable(wrapper.getTableName())) {
            db.execute(
                    """
                    create table %s.%s
                    (
                        a_temp_column int
                    ) %s ;
                    """
                            .formatted(schema, name, inheritSQL(inherits)));

            result = true;
            info.getSchema(schema)
                    .addTable(new DBTable(db.dsl()).setTableName(name).setSchema(schema));
        }

        DBTable dbTable = info.getSchema(schema).getTable(wrapper.getTableName());

        if (wrapper.getComment() != null) {
            db.execute(
                    "COMMENT ON table %s.%s is '%s'".formatted(schema, name, wrapper.getComment()));
        }

        if (wrapper.getPrimaryKey() != null) {
            checkAndBuildColumn(dbTable, wrapper.getPrimaryKey());
            dbTable.resetColumns();
        }

        wrapper.getColumns()
                .forEach(
                        column -> {
                            checkAndBuildColumn(dbTable, column);
                        });

        if (result) {
            db.execute("alter table %s.%s drop column a_temp_column;".formatted(schema, name));
        }

        dbTable.resetColumns();

        wrapper.getIndexes()
                .forEach(
                        index -> {
                            checkAndBuildIndex(dbTable, index);
                        });

        dbTable.resetIndexes();

        return result;
    }

    private boolean checkAndBuildColumn(DBTable dbTable, Column column) {
        boolean result = false;
        String name = column.getName();
        ColumnType dataType = column.getType();
        String type = getColumnTypeTransform().transform(dataType);
        Object defaultValue = column.getDefaultValue();
        String comment = column.getComment();
        boolean sequence = column.isSequence();
        boolean nullable = column.isNullable();
        boolean primaryKey = column.isPrimaryKey();

        if (!dbTable.contains(name)) {
            db.execute(
                    "alter table %s.%s add %s %s"
                            .formatted(dbTable.getSchema(), dbTable.getTableName(), name, type));
            dbTable.resetColumns();
            result = true;
        }

        DBColumn dbColumn = dbTable.getColumn(name);

        if (!dbColumn.isSequence() && !Objects.equals(dbColumn.getDefaultValue(), defaultValue)) {
            if ("varchar".equalsIgnoreCase(type)
                    && defaultValue instanceof String value
                    && !value.contains("(")
                    && !value.contains(")")) {
                defaultValue = "'%s'".formatted(value);
            }

            db.execute(
                    "alter table %s.%s alter column %s set default %s"
                            .formatted(
                                    dbTable.getSchema(),
                                    dbTable.getTableName(),
                                    name,
                                    defaultValue));
        }

        if (!Objects.equals(dbColumn.getDescription(), comment)) {
            db.execute(
                    "comment on column %s.%s.%s is '%s'"
                            .formatted(dbTable.getSchema(), dbTable.getTableName(), name, comment));
        }

        if (dbColumn.isNullable() != nullable) {
            String flag = nullable ? "drop" : "set";
            db.execute(
                    "alter table %s.%s alter column %s %s not null"
                            .formatted(dbTable.getSchema(), dbTable.getTableName(), name, flag));
        }

        if (dbColumn.isSequence() != sequence) {
            var seq = "%s_%s_seq".formatted(dbTable.getTableName(), name);
            if (sequence) {
                db.execute(
                        "create sequence if not exists %s.%s;".formatted(dbTable.getSchema(), seq));
                db.execute(
                        "alter table %s.%s alter column %s set default nextval('%s.%s')"
                                .formatted(
                                        dbTable.getSchema(),
                                        dbTable.getTableName(),
                                        name,
                                        dbTable.getSchema(),
                                        seq));
            } else {
                db.execute(
                        "alter table %s.%s alter column %s drop default"
                                .formatted(dbTable.getSchema(), dbTable.getTableName(), name));
                db.execute("drop sequence if exists %s.%s;".formatted(dbTable.getSchema(), seq));
            }
        }

        if (primaryKey && !dbColumn.isPrimaryKey()) {
            db.execute(
                    """
                    alter table %s.%s add primary key (%s)
                    """
                            .formatted(dbTable.getSchema(), dbTable.getTableName(), name));
        }

        return result;
    }

    private boolean checkAndBuildIndex(DBTable dbTable, Index index) {
        List<String> columns = index.getColumns();
        List<DBIndex> indexList = dbTable.getIndexList();
        Optional<DBIndex> dbIndexOptional =
                indexList.stream()
                        .filter(i -> new HashSet<>(i.getColumns()).equals(new HashSet<>(columns)))
                        .findFirst();

        if (dbIndexOptional.isPresent()) {
            DBIndex dbIndex = dbIndexOptional.get();
            if (dbIndex.isUnique() == index.isUnique()) {
                return false;
            } else {
                db.execute("drop index %s.%s;".formatted(dbTable.getSchema(), dbIndex.getName()));
            }
        }

        String indexName = "%s_%s_".formatted(dbTable.getTableName(), String.join("_", columns));
        String unique = "";
        String nameSuffix = "index";
        if (index.isUnique()) {
            unique = "unique";
            nameSuffix = "uindex";
        }
        db.execute(
                "create %s index if not exists %s%s on %s.%s(%s);"
                        .formatted(
                                unique,
                                indexName,
                                nameSuffix,
                                dbTable.getSchema(),
                                dbTable.getTableName(),
                                String.join(",", columns)));

        return true;
    }

    private String inheritSQL(List<TableBase> inherits) {
        if (inherits == null || inherits.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("inherits (");
        inherits.forEach(
                inherit -> {
                    builder.append("%s.%s".formatted(inherit.getSchema(), inherit.getTableName()));
                });
        builder.append(")");
        return builder.toString();
    }

    private IColumnTypeTransform getColumnTypeTransform() {
        String dbName = info.getName().toUpperCase();
        switch (dbName) {
            case "POSTGRESQL":
                return IColumnTypeTransform.POSTGRES;
            default:
                return null;
        }
    }
}
