package org.carl.infrastructure.persistence.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * schema 层级的元数据快照。
 *
 * <p>该对象只描述 schema 自身以及它包含的表，不负责任何数据库访问或缓存刷新。
 */
@Deprecated
public class DBSchema {

    private final String name;
    private String catalogName;
    private String comment;
    private String owner;
    private final Map<String, DBTable> tables = new LinkedHashMap<>();

    public DBSchema(String name) {
        this.name = name;
    }

    public void addTable(DBTable table) {
        if (table == null || table.getTableName() == null) {
            return;
        }
        this.tables.put(table.getTableName(), table);
    }

    public DBSchema setTables(Map<String, DBTable> tables) {
        this.tables.clear();
        if (tables != null) {
            tables.values().forEach(this::addTable);
        }
        return this;
    }

    public DBTable getTable(String name) {
        if (name == null) {
            return null;
        }

        String quotedLookup = MetadataNames.quotedIdentifierValue(name);
        DBTable table = quotedLookup == null ? null : this.tables.get(quotedLookup);
        if (table != null) {
            return table;
        }

        return this.tables.values().stream()
                .filter(candidate -> MetadataNames.matches(candidate.getTableName(), name))
                .findFirst()
                .orElse(null);
    }

    public String getName() {
        return name;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public DBSchema setCatalogName(String catalogName) {
        this.catalogName = catalogName;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public DBSchema setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public DBSchema setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DBSchema && name.equals(((DBSchema) obj).name);
    }

    public Map<String, DBTable> getTables() {
        return Collections.unmodifiableMap(tables);
    }

    public boolean containsTable(String tableName) {
        return getTable(tableName) != null;
    }
}
