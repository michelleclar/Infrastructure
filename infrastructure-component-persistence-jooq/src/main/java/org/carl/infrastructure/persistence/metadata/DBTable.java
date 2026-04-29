package org.carl.infrastructure.persistence.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 表级元数据快照。
 *
 * <p>这里聚合列、主键、索引和 check constraint 等结构信息，供历史建表/比对逻辑继续消费。
 */
@Deprecated
public class DBTable {
    private String catalogName;
    private String schema;
    private String tableName;
    private String tableType;
    private String comment;
    private String owner;
    private final Map<String, DBColumn> columnMap = new LinkedHashMap<>();
    private final Map<String, DBColumn> columnLookup = new LinkedHashMap<>();
    private final List<String> primaryKeyColumns = new ArrayList<>();
    private final List<DBIndex> indexList = new ArrayList<>();
    private final List<DBCheckConstraint> checkConstraints = new ArrayList<>();

    public DBTable setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public DBTable setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public DBTable setCatalogName(String catalogName) {
        this.catalogName = catalogName;
        return this;
    }

    public String getSchema() {
        return schema;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSchemaDotTable() {
        return schema + "." + tableName;
    }

    public String getTableType() {
        return tableType;
    }

    public DBTable setTableType(String tableType) {
        this.tableType = tableType;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public DBTable setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public DBTable setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public Map<String, DBColumn> getColumnMap() {
        return Collections.unmodifiableMap(columnMap);
    }

    public DBTable setColumnMap(Map<String, DBColumn> columns) {
        this.columnMap.clear();
        this.columnLookup.clear();
        if (columns != null) {
            columns.values().forEach(this::addColumn);
        }
        return this;
    }

    public DBTable addColumn(DBColumn column) {
        if (column == null || column.getName() == null) {
            return this;
        }
        this.columnMap.put(column.getName(), column);
        // Only store a folded lookup key for identifiers that PostgreSQL would also
        // fold implicitly. Quoted mixed-case names must stay exact-only.
        String lookupKey = MetadataNames.normalizeStoredName(column.getName());
        if (lookupKey != null) {
            this.columnLookup.put(lookupKey, column);
        }
        if (column.isPrimaryKey() && !primaryKeyColumns.contains(column.getName())) {
            this.primaryKeyColumns.add(column.getName());
        }
        return this;
    }

    public List<String> getPrimaryKeyColumns() {
        return Collections.unmodifiableList(primaryKeyColumns);
    }

    public DBTable setPrimaryKeyColumns(List<String> primaryKeys) {
        this.primaryKeyColumns.clear();
        if (primaryKeys != null) {
            primaryKeys.forEach(
                    primaryKey -> {
                        if (primaryKey != null && !this.primaryKeyColumns.contains(primaryKey)) {
                            this.primaryKeyColumns.add(primaryKey);
                        }
                    });
        }
        return this;
    }

    public List<DBIndex> getIndexList() {
        return Collections.unmodifiableList(indexList);
    }

    public DBTable setIndexList(List<DBIndex> indexList) {
        this.indexList.clear();
        if (indexList != null) {
            this.indexList.addAll(indexList);
        }
        return this;
    }

    public DBTable addIndex(DBIndex index) {
        if (index != null) {
            this.indexList.add(index);
        }
        return this;
    }

    public List<DBCheckConstraint> getCheckConstraints() {
        return Collections.unmodifiableList(checkConstraints);
    }

    public DBTable setCheckConstraints(List<DBCheckConstraint> checkConstraints) {
        this.checkConstraints.clear();
        if (checkConstraints != null) {
            this.checkConstraints.addAll(checkConstraints);
        }
        return this;
    }

    public DBTable addCheckConstraint(DBCheckConstraint checkConstraint) {
        if (checkConstraint != null) {
            this.checkConstraints.add(checkConstraint);
        }
        return this;
    }

    public boolean contains(String column) {
        if (column == null) {
            return false;
        }
        return getColumn(column) != null;
    }

    public DBColumn getColumn(String column) {
        Objects.requireNonNull(column);
        // A quoted lookup should target the exact identifier text inside the quotes.
        String exactLookup = MetadataNames.quotedIdentifierValue(column);
        DBColumn hit = exactLookup == null ? null : columnMap.get(exactLookup);
        if (hit != null) {
            return hit;
        }
        String lookupKey = MetadataNames.normalizeLookupName(column);
        if (lookupKey == null) {
            return null;
        }
        return columnLookup.get(lookupKey);
    }
}
