package org.carl.infrastructure.persistence.metadata;

import org.carl.infrastructure.persistence.builder.TableBase;
import org.carl.infrastructure.persistence.core.PersistenceContext;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Deprecated
public class DBTable extends TableBase {
    private PersistenceContext dsl;

    private Map<String, DBColumn> columnMap;

    private List<DBIndex> indexList;

    public DBTable(PersistenceContext dsl) {
        this.dsl = dsl;
    }

    public DBTable setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public DBTable setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public Map<String, DBColumn> getColumnMap() {
        if (columnMap == null) {
            dsl.connection(
                    connection -> {
                        try {
                            DatabaseMetaData metaData = connection.getMetaData();
                            Map<String, DBColumn> columnMap = new HashMap<>();
                            try (ResultSet rs =
                                    metaData.getColumns(null, schema, tableName, null)) {
                                while (rs.next()) {
                                    DBColumn column = new DBColumn();
                                    column.setName(rs.getString("COLUMN_NAME"));
                                    column.setType(rs.getString("TYPE_NAME"));
                                    column.setNullable(
                                            rs.getInt("NULLABLE")
                                                    == DatabaseMetaData.columnNullable);
                                    var defaultValue = rs.getString("COLUMN_DEF");
                                    column.setDefaultValue(defaultValue);
                                    if ("YES".equals(rs.getString("IS_AUTOINCREMENT"))) {
                                        column.setSequence();
                                    }
                                    if (defaultValue != null
                                            && defaultValue.startsWith("nextval(")) {
                                        column.setSequence();
                                    }
                                    column.setDescription(rs.getString("REMARKS"));

                                    columnMap.put(column.getName(), column);
                                }
                            }

                            // Primary keys
                            try (ResultSet rs = metaData.getPrimaryKeys(null, schema, tableName)) {
                                while (rs.next()) {
                                    String primaryKeyColumn = rs.getString("COLUMN_NAME");
                                    DBColumn column = columnMap.get(primaryKeyColumn);
                                    if (column != null) {
                                        column.setPrimaryKey(true);
                                    }
                                }
                            }

                            this.columnMap = columnMap;
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return columnMap;
    }

    public List<DBIndex> getIndexList() {
        if (indexList == null) {
            indexList = new ArrayList<>();
            dsl.connection(
                    connection -> {
                        try {
                            DatabaseMetaData metaData = connection.getMetaData();
                            try (ResultSet rs =
                                    metaData.getIndexInfo(null, schema, tableName, false, false)) {
                                while (rs.next()) {
                                    String indexName = rs.getString("INDEX_NAME");
                                    if (indexName.endsWith("_pkey")) { // 主键索引不参与
                                        continue;
                                    }

                                    String columnName = rs.getString("COLUMN_NAME");
                                    Optional<DBIndex> hitIndex =
                                            indexList.stream()
                                                    .filter(i -> i.getName().equals(indexName))
                                                    .findFirst();

                                    if (hitIndex.isPresent()) {
                                        hitIndex.get().addColumn(columnName);
                                    } else {
                                        DBIndex index = new DBIndex();
                                        index.setName(indexName);
                                        index.addColumn(columnName);
                                        if (!rs.getBoolean("NON_UNIQUE")) {
                                            index.setUnique(true);
                                        }
                                        indexList.add(index);
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return indexList;
    }

    public boolean contains(String column) {
        if (column == null) {
            return false;
        }
        return getColumn(column.toLowerCase()) != null;
    }

    public DBColumn getColumn(String column) {
        Objects.requireNonNull(column);
        return getColumnMap().get(column.toLowerCase());
    }

    public void resetColumns() {
        this.columnMap = null;
    }

    public void resetIndexes() {
        this.indexList = null;
    }
}
