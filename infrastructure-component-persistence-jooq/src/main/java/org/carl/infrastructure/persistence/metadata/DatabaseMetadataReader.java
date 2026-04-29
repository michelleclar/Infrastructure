package org.carl.infrastructure.persistence.metadata;

import org.carl.infrastructure.persistence.core.PersistenceContext;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 从 {@link org.carl.infrastructure.persistence.core.PersistenceContext} 读取数据库结构快照的入口。
 *
 * <p>读取顺序分成两层：先走 JDBC {@link DatabaseMetaData} 获取可移植的基础信息，再按 PostgreSQL
 * catalog 补充 owner、expression index、include column、check constraint 等细节。
 */
public class DatabaseMetadataReader {
    private static final String[] TABLE_TYPES = new String[] {"TABLE"};
    private static final String POSTGRESQL = "PostgreSQL";
    private static final String POSTGRES_SCHEMA_METADATA_SQL =
            """
            select n.nspname as schema_name,
                   r.rolname as owner,
                   obj_description(n.oid, 'pg_namespace') as comment
              from pg_namespace n
              join pg_roles r on r.oid = n.nspowner
            """;
    private static final String POSTGRES_TABLE_METADATA_SQL =
            """
            select n.nspname as schema_name,
                   c.relname as table_name,
                   r.rolname as owner,
                   obj_description(c.oid, 'pg_class') as comment,
                   c.relkind as rel_kind
              from pg_class c
              join pg_namespace n on n.oid = c.relnamespace
              join pg_roles r on r.oid = c.relowner
             where c.relkind in ('r', 'p', 'v', 'm', 'f')
            """;
    private static final String POSTGRES_TABLE_CHECK_SQL =
            """
            select con.conname as constraint_name,
                   pg_get_constraintdef(con.oid, true) as definition
              from pg_constraint con
              join pg_class rel on rel.oid = con.conrelid
              join pg_namespace nsp on nsp.oid = rel.relnamespace
             where nsp.nspname = ?
               and rel.relname = ?
               and con.contype = 'c'
             order by con.conname
            """;
    private static final String POSTGRES_INDEX_METADATA_SQL =
            """
            select idx.relname as index_name,
                   i.indisunique as is_unique,
                   am.amname as access_method,
                   pg_get_expr(i.indpred, i.indrelid) as predicate,
                   pg_get_indexdef(idx.oid) as definition,
                   key.ordinality::int as ordinal_position,
                   att.attname as column_name,
                   pg_get_indexdef(idx.oid, key.ordinality::int, true) as expression,
                   key.ordinality > i.indnkeyatts as included
              from pg_index i
              join pg_class idx on idx.oid = i.indexrelid
              join pg_class tbl on tbl.oid = i.indrelid
              join pg_namespace nsp on nsp.oid = tbl.relnamespace
              join pg_am am on am.oid = idx.relam
              join unnest(i.indkey) with ordinality as key(attnum, ordinality) on true
              left join pg_attribute att on att.attrelid = tbl.oid and att.attnum = key.attnum
             where nsp.nspname = ?
               and tbl.relname = ?
               and idx.relname not like '%_pkey'
             order by idx.relname, key.ordinality
            """;

    private final PersistenceContext persistenceContext;

    public DatabaseMetadataReader(PersistenceContext persistenceContext) {
        this.persistenceContext = Objects.requireNonNull(persistenceContext);
    }

    /** 读取当前连接可见的数据库级快照。 */
    public DBInfo getDBInfo() {
        return persistenceContext.connectionResult(
                connection -> {
                    try {
                        DatabaseMetaData metaData = connection.getMetaData();
                        DBInfo info =
                                new DBInfo(
                                                metaData.getDatabaseProductName(),
                                                metaData.getDatabaseProductVersion())
                                        .setDialect(metaData.getDatabaseProductName())
                                        .setDefaultSchema(connection.getSchema());

                        try (ResultSet schemasRs = metaData.getSchemas()) {
                            while (schemasRs.next()) {
                                String schemaName = schemasRs.getString("TABLE_SCHEM");
                                info.addSchema(
                                        new DBSchema(schemaName)
                                                .setCatalogName(schemasRs.getString("TABLE_CATALOG")));
                            }
                        }

                        try (ResultSet tablesRs =
                                metaData.getTables(null, null, null, TABLE_TYPES)) {
                            while (tablesRs.next()) {
                                String schemaName = tablesRs.getString("TABLE_SCHEM");
                                info.getOrAddSchema(schemaName)
                                        .addTable(
                                                new DBTable()
                                                        .setCatalogName(tablesRs.getString("TABLE_CAT"))
                                                        .setSchema(schemaName)
                                                        .setTableName(tablesRs.getString("TABLE_NAME"))
                                                        .setTableType(tablesRs.getString("TABLE_TYPE"))
                                                        .setComment(tablesRs.getString("REMARKS")));
                            }
                        }

                        // JDBC metadata gives us the portable baseline; PostgreSQL
                        // catalog queries fill in details such as owner and relkind.
                        if (isPostgres(metaData)) {
                            populatePostgresSchemaMetadata(connection, info);
                            populatePostgresTableMetadata(connection, info);
                        }

                        return info;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** 读取指定 schema/table 的完整表级快照；如果表不存在则返回 {@code null}。 */
    public DBTable getTable(String schema, String tableName) {
        return persistenceContext.connectionResult(
                connection -> {
                    try {
                        DatabaseMetaData metaData = connection.getMetaData();
                        DBTable table = loadTable(metaData, schema, tableName);
                        if (table == null) {
                            return null;
                        }

                        populateColumns(metaData, table);
                        populateIndexes(metaData, table);
                        // PostgreSQL-specific metadata is layered on top so callers still
                        // get a useful snapshot on other JDBC dialects.
                        if (isPostgres(metaData)) {
                            populatePostgresTableMetadata(connection, table);
                            populatePostgresCheckConstraints(connection, table);
                            populatePostgresIndexMetadata(connection, table);
                        }
                        return table;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /** 仅返回表的列快照视图，供旧调用链按原接口读取。 */
    public Map<String, DBColumn> getColumnMap(String schema, String tableName) {
        DBTable table = getTable(schema, tableName);
        if (table == null) {
            return Map.of();
        }
        return table.getColumnMap();
    }

    /** 在 JDBC 返回的表列表里按当前 identifier 规则定位目标表。 */
    private DBTable loadTable(DatabaseMetaData metaData, String schema, String tableName)
            throws SQLException {
        try (ResultSet tablesRs = metaData.getTables(null, schema, null, TABLE_TYPES)) {
            while (tablesRs.next()) {
                String candidateName = tablesRs.getString("TABLE_NAME");
                if (!MetadataNames.matches(candidateName, tableName)) {
                    continue;
                }

                return new DBTable()
                        .setCatalogName(tablesRs.getString("TABLE_CAT"))
                        .setSchema(tablesRs.getString("TABLE_SCHEM"))
                        .setTableName(candidateName)
                        .setTableType(tablesRs.getString("TABLE_TYPE"))
                        .setComment(tablesRs.getString("REMARKS"));
            }
        }

        return null;
    }

    /** 基于 JDBC {@link DatabaseMetaData#getColumns} 和主键结果集构建列快照。 */
    private void populateColumns(DatabaseMetaData metaData, DBTable table) throws SQLException {
        Map<String, DBColumn> columns = new LinkedHashMap<>();
        try (ResultSet rs = metaData.getColumns(null, table.getSchema(), table.getTableName(), null)) {
            while (rs.next()) {
                DBColumn column = new DBColumn();
                column.setName(rs.getString("COLUMN_NAME"));
                column.setType(rs.getString("TYPE_NAME"));
                column.setJdbcType(readInt(rs, "DATA_TYPE"));
                column.setOrdinalPosition(readInt(rs, "ORDINAL_POSITION"));
                column.setColumnSize(readInt(rs, "COLUMN_SIZE"));
                column.setDecimalDigits(readInt(rs, "DECIMAL_DIGITS"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setRawDefaultValue(rs.getString("COLUMN_DEF"));
                column.setDescription(rs.getString("REMARKS"));

                boolean autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                column.setAutoIncrement(autoIncrement);
                column.setGenerated("YES".equalsIgnoreCase(rs.getString("IS_GENERATEDCOLUMN")));

                if (autoIncrement
                        || (column.getRawDefaultValue() != null
                                && column.getRawDefaultValue().startsWith("nextval("))) {
                    column.setSequence(true);
                }

                columns.put(column.getName(), column);
            }
        }

        TreeMap<Integer, String> primaryKeys = new TreeMap<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, table.getSchema(), table.getTableName())) {
            while (rs.next()) {
                String primaryKeyColumn = rs.getString("COLUMN_NAME");
                DBColumn column = columns.get(primaryKeyColumn);
                if (column != null) {
                    column.setPrimaryKey(true);
                }

                Integer keySequence = readInt(rs, "KEY_SEQ");
                if (keySequence == null) {
                    keySequence = primaryKeys.size() + 1;
                }
                primaryKeys.put(keySequence, primaryKeyColumn);
            }
        }

        table.setColumnMap(columns);
        table.setPrimaryKeyColumns(new ArrayList<>(primaryKeys.values()));
    }

    /** 基于 JDBC 索引元数据构建一个可移植但信息较少的索引快照。 */
    private void populateIndexes(DatabaseMetaData metaData, DBTable table) throws SQLException {
        Map<String, DBIndex> indexes = new LinkedHashMap<>();
        try (ResultSet rs =
                metaData.getIndexInfo(null, table.getSchema(), table.getTableName(), false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                if (indexName == null || columnName == null || indexName.endsWith("_pkey")) {
                    continue;
                }

                // JDBC only exposes key columns here, which is still the right fallback
                // for non-PostgreSQL dialects and for legacy callers.
                DBIndex index = indexes.get(indexName);
                if (index == null) {
                    index =
                            new DBIndex()
                                    .setName(indexName)
                                    .setUnique(!rs.getBoolean("NON_UNIQUE"))
                                    .setIndexType(resolveIndexType(readInt(rs, "TYPE")));
                    indexes.put(indexName, index);
                }
                index.addColumn(
                        new DBIndexColumn(
                                columnName,
                                readInt(rs, "ORDINAL_POSITION"),
                                rs.getString("ASC_OR_DESC")));
            }
        }
        table.setIndexList(new ArrayList<>(indexes.values()));
    }

    private Integer readInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private String resolveIndexType(Integer type) {
        if (type == null) {
            return null;
        }
        int value = type;
        return switch (value) {
            case DatabaseMetaData.tableIndexClustered -> "clustered";
            case DatabaseMetaData.tableIndexHashed -> "hashed";
            case DatabaseMetaData.tableIndexStatistic -> "statistic";
            case DatabaseMetaData.tableIndexOther -> "other";
            default -> null;
        };
    }

    private boolean isPostgres(DatabaseMetaData metaData) throws SQLException {
        return POSTGRESQL.equalsIgnoreCase(metaData.getDatabaseProductName());
    }

    /** 读取 PostgreSQL catalog 中的 schema owner 和注释。 */
    private void populatePostgresSchemaMetadata(Connection connection, DBInfo info)
            throws SQLException {
        try (PreparedStatement statement =
                        connection.prepareStatement(POSTGRES_SCHEMA_METADATA_SQL);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                DBSchema schema = info.getSchema(rs.getString("schema_name"));
                if (schema == null) {
                    continue;
                }
                schema.setOwner(rs.getString("owner")).setComment(rs.getString("comment"));
            }
        }
    }

    /** 为数据库级快照中的表补齐 PostgreSQL owner、comment 和 relkind 信息。 */
    private void populatePostgresTableMetadata(Connection connection, DBInfo info)
            throws SQLException {
        try (PreparedStatement statement =
                        connection.prepareStatement(POSTGRES_TABLE_METADATA_SQL);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                DBSchema schema = info.getSchema(rs.getString("schema_name"));
                if (schema == null) {
                    continue;
                }
                DBTable table = schema.getTable(rs.getString("table_name"));
                if (table == null) {
                    continue;
                }
                table.setOwner(rs.getString("owner"))
                        .setComment(rs.getString("comment"))
                        .setTableType(resolvePostgresTableType(rs.getString("rel_kind")));
            }
        }
    }

    /** 为单张表补齐 PostgreSQL owner、comment 和 relkind 信息。 */
    private void populatePostgresTableMetadata(Connection connection, DBTable table)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement(
                        POSTGRES_TABLE_METADATA_SQL + " and n.nspname = ? and c.relname = ?")) {
            statement.setString(1, table.getSchema());
            statement.setString(2, table.getTableName());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    table.setOwner(rs.getString("owner"))
                            .setComment(rs.getString("comment"))
                            .setTableType(resolvePostgresTableType(rs.getString("rel_kind")));
                }
            }
        }
    }

    /** 读取 PostgreSQL check constraint，并挂到表快照上。 */
    private void populatePostgresCheckConstraints(Connection connection, DBTable table)
            throws SQLException {
        List<DBCheckConstraint> checkConstraints = new ArrayList<>();
        try (PreparedStatement statement =
                connection.prepareStatement(POSTGRES_TABLE_CHECK_SQL)) {
            statement.setString(1, table.getSchema());
            statement.setString(2, table.getTableName());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    checkConstraints.add(
                            new DBCheckConstraint(
                                    rs.getString("constraint_name"),
                                    rs.getString("definition")));
                }
            }
        }
        table.setCheckConstraints(checkConstraints);
    }

    /** 读取 PostgreSQL 特有的索引细节，覆盖 JDBC fallback 中缺失的字段。 */
    private void populatePostgresIndexMetadata(Connection connection, DBTable table)
            throws SQLException {
        Map<String, DBIndex> currentIndexes = new LinkedHashMap<>();
        table.getIndexList().forEach(index -> currentIndexes.put(index.getName(), index));
        Map<String, List<DBIndexColumn>> postgresColumns = new LinkedHashMap<>();

        try (PreparedStatement statement =
                connection.prepareStatement(POSTGRES_INDEX_METADATA_SQL)) {
            statement.setString(1, table.getSchema());
            statement.setString(2, table.getTableName());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    DBIndex index = currentIndexes.get(indexName);
                    if (index == null) {
                        // Expression-only indexes might not appear in the JDBC fallback
                        // because COLUMN_NAME is null, so build the shell here.
                        index = new DBIndex().setName(indexName);
                        currentIndexes.put(indexName, index);
                    }
                    Integer ordinalPosition = readInt(rs, "ordinal_position");
                    index.setUnique(rs.getBoolean("is_unique"))
                            .setIndexType(rs.getString("access_method"))
                            .setPredicate(rs.getString("predicate"))
                            .setDefinition(rs.getString("definition"));
                    postgresColumns
                            .computeIfAbsent(indexName, ignored -> new ArrayList<>())
                            .add(
                                    new DBIndexColumn(
                                            rs.getString("column_name"),
                                            ordinalPosition,
                                            findExistingSortOrder(index, ordinalPosition),
                                            rs.getString("expression"),
                                            rs.getBoolean("included")));
                }
            }
        }

        currentIndexes.forEach(
                (indexName, index) -> {
                    List<DBIndexColumn> columns = postgresColumns.get(indexName);
                    if (columns != null && !columns.isEmpty()) {
                        index.setColumns(columns);
                    }
                });
        table.setIndexList(new ArrayList<>(currentIndexes.values()));
    }

    private String findExistingSortOrder(DBIndex index, Integer ordinalPosition) {
        if (ordinalPosition == null) {
            return null;
        }
        return index.getColumnDetails().stream()
                .filter(column -> ordinalPosition.equals(column.getOrdinalPosition()))
                .map(DBIndexColumn::getSortOrder)
                .findFirst()
                .orElse(null);
    }

    private String resolvePostgresTableType(String relKind) {
        if (relKind == null) {
            return null;
        }
        return switch (relKind) {
            case "r" -> "TABLE";
            case "p" -> "PARTITIONED TABLE";
            case "v" -> "VIEW";
            case "m" -> "MATERIALIZED VIEW";
            case "f" -> "FOREIGN TABLE";
            default -> relKind;
        };
    }
}
