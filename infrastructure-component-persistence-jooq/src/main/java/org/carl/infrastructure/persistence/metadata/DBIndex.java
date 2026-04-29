package org.carl.infrastructure.persistence.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 数据库索引的元数据快照。
 *
 * <p>既保留旧调用链常用的 key-column 视图，也暴露 PostgreSQL expression/include 等更完整的列定义。
 */
@Deprecated
public class DBIndex {
    private String name;
    private boolean unique = false;
    private String indexType;
    private String predicate;
    private String definition;
    private final List<DBIndexColumn> columns = new ArrayList<>();

    public String getName() {
        return name;
    }

    public DBIndex setName(String name) {
        this.name = name;
        return this;
    }

    public boolean isUnique() {
        return unique;
    }

    public DBIndex setUnique(boolean unique) {
        this.unique = unique;
        return this;
    }

    public String getIndexType() {
        return indexType;
    }

    public DBIndex setIndexType(String indexType) {
        this.indexType = indexType;
        return this;
    }

    public String getPredicate() {
        return predicate;
    }

    public DBIndex setPredicate(String predicate) {
        this.predicate = predicate;
        return this;
    }

    public String getDefinition() {
        return definition;
    }

    public DBIndex setDefinition(String definition) {
        this.definition = definition;
        return this;
    }

    public List<String> getColumns() {
        // Keep the legacy contract focused on key columns so old diff logic does not
        // treat PostgreSQL INCLUDE columns as part of the index key.
        return columns.stream()
                .filter(column -> !column.isIncluded())
                .map(DBIndexColumn::getReferenceName)
                .toList();
    }

    public List<DBIndexColumn> getColumnDetails() {
        return Collections.unmodifiableList(columns);
    }

    public DBIndex addColumn(String columnName) {
        return addColumn(new DBIndexColumn(columnName, columns.size() + 1, null));
    }

    public DBIndex addColumn(DBIndexColumn column) {
        if (column == null) {
            return this;
        }
        if (column.getName() == null && column.getExpression() == null) {
            return this;
        }
        this.columns.add(column);
        sortColumns();
        return this;
    }

    public DBIndex setColumns(List<DBIndexColumn> columns) {
        this.columns.clear();
        if (columns != null) {
            this.columns.addAll(columns);
        }
        sortColumns();
        return this;
    }

    private void sortColumns() {
        this.columns.sort(
                Comparator.comparing(
                        DBIndexColumn::getOrdinalPosition,
                        Comparator.nullsLast(Integer::compareTo)));
    }

    public boolean isMulti() {
        return columns.size() > 1;
    }
}
