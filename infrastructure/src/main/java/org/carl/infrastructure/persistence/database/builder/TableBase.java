package org.carl.infrastructure.persistence.database.builder;
@Deprecated
public class TableBase {
    protected String schema;
    protected String tableName;

    public TableBase() {}

    public TableBase(String schema, String name) {
        this.schema = schema;
        this.tableName = name;
    }

    public TableBase setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public TableBase setTableName(String tableName) {
        this.tableName = tableName;
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
}
