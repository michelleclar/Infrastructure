package org.carl.infrastructure.persistence.utils;

import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.CheckConstraintInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ColumnInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ColumnTypeFamily;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.DatabaseInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ForeignKeyInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.IndexColumnInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.IndexInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.NormalizedColumnType;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.PrimaryKeyInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.SchemaInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.TableInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * External-target metadata entry point.
 */
public final class ExternalDatabaseMetadataReader {
    private static final String[] TABLE_TYPES = new String[] {"TABLE"};

    private final ExternalJdbcExecutor executor;

    public ExternalDatabaseMetadataReader(ExternalJdbcExecutor executor) {
        this.executor = executor;
    }

    public ExternalDatabaseResult<DatabaseInfo> readDatabase(
            ExternalDatabaseConfig config, Duration timeout) {
        return executor.executeSql(config, timeout, this::readDatabase);
    }

    public ExternalDatabaseResult<List<SchemaInfo>> listSchemas(
            ExternalDatabaseConfig config, Duration timeout) {
        return executor.executeSql(config, timeout, connection -> readSchemas(connection, false));
    }

    public ExternalDatabaseResult<List<TableInfo>> listTables(
            ExternalDatabaseConfig config, Duration timeout, String schemaName) {
        return executor.executeSql(config, timeout, connection -> readTableList(connection, schemaName));
    }

    public ExternalDatabaseResult<TableInfo> readTable(
            ExternalDatabaseConfig config, Duration timeout, String schemaName, String tableName) {
        return executor.executeSql(
                config, timeout, connection -> readTable(connection, schemaName, tableName));
    }

    private DatabaseInfo readDatabase(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        return new DatabaseInfo(
                metaData.getDatabaseProductName(),
                metaData.getDatabaseProductVersion(),
                readDefaultSchema(connection),
                readSchemas(connection, true));
    }

    private List<SchemaInfo> readSchemas(Connection connection, boolean includeTables)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<SchemaInfo> schemas = new ArrayList<>();
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                String catalogName = rs.getString("TABLE_CATALOG");
                String schemaName = rs.getString("TABLE_SCHEM");
                List<TableInfo> tables =
                        includeTables ? readTableList(connection, schemaName) : List.of();
                schemas.add(new SchemaInfo(catalogName, schemaName, null, tables));
            }
        }
        if (schemas.isEmpty()) {
            try (ResultSet rs = metaData.getCatalogs()) {
                while (rs.next()) {
                    String catalogName = rs.getString("TABLE_CAT");
                    List<TableInfo> tables =
                            includeTables ? readTableList(connection, catalogName) : List.of();
                    schemas.add(new SchemaInfo(catalogName, catalogName, null, tables));
                }
            }
        }
        return schemas;
    }

    private List<TableInfo> readTableList(Connection connection, String schemaName)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        List<TableInfo> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, schemaName, null, TABLE_TYPES)) {
            while (rs.next()) {
                tables.add(
                        new TableInfo(
                                rs.getString("TABLE_CAT"),
                                rs.getString("TABLE_SCHEM"),
                                rs.getString("TABLE_NAME"),
                                rs.getString("TABLE_TYPE"),
                                rs.getString("REMARKS"),
                                List.of(),
                                null,
                                List.of(),
                                List.of(),
                                List.of()));
            }
        }
        if (tables.isEmpty() && schemaName != null) {
            try (ResultSet rs = metaData.getTables(schemaName, null, null, TABLE_TYPES)) {
                while (rs.next()) {
                    tables.add(
                            new TableInfo(
                                    rs.getString("TABLE_CAT"),
                                    rs.getString("TABLE_SCHEM"),
                                    rs.getString("TABLE_NAME"),
                                    rs.getString("TABLE_TYPE"),
                                    rs.getString("REMARKS"),
                                    List.of(),
                                    null,
                                    List.of(),
                                    List.of(),
                                    List.of()));
                }
            }
        }
        return tables;
    }

    private TableInfo readTable(Connection connection, String schemaName, String tableName)
            throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        TableInfo table = null;
        try (ResultSet rs = metaData.getTables(null, schemaName, tableName, TABLE_TYPES)) {
            if (rs.next()) {
                table =
                        new TableInfo(
                                rs.getString("TABLE_CAT"),
                                rs.getString("TABLE_SCHEM"),
                                rs.getString("TABLE_NAME"),
                                rs.getString("TABLE_TYPE"),
                                rs.getString("REMARKS"),
                                List.of(),
                                null,
                                List.of(),
                                List.of(),
                                List.of());
            }
        }
        if (table == null && schemaName != null) {
            try (ResultSet rs = metaData.getTables(schemaName, null, tableName, TABLE_TYPES)) {
                if (rs.next()) {
                    table =
                            new TableInfo(
                                    rs.getString("TABLE_CAT"),
                                    rs.getString("TABLE_SCHEM"),
                                    rs.getString("TABLE_NAME"),
                                    rs.getString("TABLE_TYPE"),
                                    rs.getString("REMARKS"),
                                    List.of(),
                                    null,
                                    List.of(),
                                    List.of(),
                                    List.of());
                }
            }
        }
        if (table == null) {
            return null;
        }

        return new TableInfo(
                table.catalogName(),
                table.schemaName(),
                table.tableName(),
                table.tableType(),
                table.comment(),
                readColumns(metaData, table),
                readPrimaryKey(metaData, table),
                readForeignKeys(metaData, table),
                readIndexes(metaData, table),
                List.of());
    }

    private List<ColumnInfo> readColumns(DatabaseMetaData metaData, TableInfo table)
            throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (ResultSet rs =
                metaData.getColumns(null, table.schemaName(), table.tableName(), null)) {
            readColumnRows(rs, columns);
        }
        if (columns.isEmpty() && table.catalogName() != null) {
            try (ResultSet rs =
                    metaData.getColumns(table.catalogName(), null, table.tableName(), null)) {
                readColumnRows(rs, columns);
            }
        }
        return columns;
    }

    private PrimaryKeyInfo readPrimaryKey(DatabaseMetaData metaData, TableInfo table)
            throws SQLException {
        TreeMap<Integer, String> columns = new TreeMap<>();
        String primaryKeyName = null;
        try (ResultSet rs =
                metaData.getPrimaryKeys(null, table.schemaName(), table.tableName())) {
            while (rs.next()) {
                if (primaryKeyName == null) {
                    primaryKeyName = rs.getString("PK_NAME");
                }
                Integer keySequence = readInt(rs, "KEY_SEQ");
                if (keySequence == null) {
                    keySequence = columns.size() + 1;
                }
                columns.put(keySequence, rs.getString("COLUMN_NAME"));
            }
        }
        if (columns.isEmpty() && table.catalogName() != null) {
            try (ResultSet rs =
                    metaData.getPrimaryKeys(table.catalogName(), null, table.tableName())) {
                while (rs.next()) {
                    if (primaryKeyName == null) {
                        primaryKeyName = rs.getString("PK_NAME");
                    }
                    Integer keySequence = readInt(rs, "KEY_SEQ");
                    if (keySequence == null) {
                        keySequence = columns.size() + 1;
                    }
                    columns.put(keySequence, rs.getString("COLUMN_NAME"));
                }
            }
        }
        if (columns.isEmpty()) {
            return null;
        }
        return new PrimaryKeyInfo(primaryKeyName, new ArrayList<>(columns.values()));
    }

    private List<ForeignKeyInfo> readForeignKeys(DatabaseMetaData metaData, TableInfo table)
            throws SQLException {
        Map<String, ForeignKeyBuilder> foreignKeys = new LinkedHashMap<>();
        try (ResultSet rs =
                metaData.getImportedKeys(null, table.schemaName(), table.tableName())) {
            readForeignKeyRows(rs, foreignKeys);
        }
        if (foreignKeys.isEmpty() && table.catalogName() != null) {
            try (ResultSet rs =
                    metaData.getImportedKeys(table.catalogName(), null, table.tableName())) {
                readForeignKeyRows(rs, foreignKeys);
            }
        }
        return foreignKeys.values().stream().map(ForeignKeyBuilder::build).toList();
    }

    private void readColumnRows(ResultSet rs, List<ColumnInfo> columns) throws SQLException {
        while (rs.next()) {
            String typeName = rs.getString("TYPE_NAME");
            Integer jdbcType = readInt(rs, "DATA_TYPE");
            Integer precision = readInt(rs, "COLUMN_SIZE");
            Integer scale = readInt(rs, "DECIMAL_DIGITS");
            boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
            boolean generated = "YES".equals(rs.getString("IS_GENERATEDCOLUMN"));
            boolean autoIncrement = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
            String defaultValue = rs.getString("COLUMN_DEF");
            NormalizedColumnType type =
                    new NormalizedColumnType(
                            typeName,
                            jdbcType,
                            precision,
                            scale,
                            nullable,
                            generated,
                            autoIncrement,
                            defaultValue,
                            resolveFamily(jdbcType, typeName));
            columns.add(
                    new ColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("REMARKS"),
                            type,
                            readInt(rs, "ORDINAL_POSITION"),
                            nullable,
                            generated,
                            autoIncrement,
                            defaultValue));
        }
    }

    private void readForeignKeyRows(ResultSet rs, Map<String, ForeignKeyBuilder> foreignKeys)
            throws SQLException {
            while (rs.next()) {
                String name = rs.getString("FK_NAME");
                String key = name == null ? "FK#" + foreignKeys.size() : name;
                ForeignKeyBuilder builder =
                        foreignKeys.computeIfAbsent(
                                key,
                                ignored ->
                                        new ForeignKeyBuilder(
                                                name,
                                                rsString(rs, "PKTABLE_CAT"),
                                                rsString(rs, "PKTABLE_SCHEM"),
                                                rsString(rs, "PKTABLE_NAME")));
                Integer keySequence = readInt(rs, "KEY_SEQ");
                if (keySequence == null) {
                    keySequence = builder.nextSequence();
                }
                builder.add(keySequence, rs.getString("FKCOLUMN_NAME"), rs.getString("PKCOLUMN_NAME"));
            }
    }

    private List<IndexInfo> readIndexes(DatabaseMetaData metaData, TableInfo table)
            throws SQLException {
        Map<String, IndexBuilder> indexes = new LinkedHashMap<>();
        try (ResultSet rs =
                metaData.getIndexInfo(null, table.schemaName(), table.tableName(), false, false)) {
            readIndexRows(rs, indexes);
        }
        if (indexes.isEmpty() && table.catalogName() != null) {
            try (ResultSet rs =
                    metaData.getIndexInfo(table.catalogName(), null, table.tableName(), false, false)) {
                readIndexRows(rs, indexes);
            }
        }
        return indexes.values().stream().map(IndexBuilder::build).toList();
    }

    private void readIndexRows(ResultSet rs, Map<String, IndexBuilder> indexes) throws SQLException {
        while (rs.next()) {
            String name = rs.getString("INDEX_NAME");
            String columnName = rs.getString("COLUMN_NAME");
            if (name == null || columnName == null) {
                continue;
            }
            IndexBuilder builder =
                    indexes.computeIfAbsent(
                            name, ignored -> new IndexBuilder(name, !rsBool(rs, "NON_UNIQUE")));
            builder.add(
                    new IndexColumnInfo(
                            columnName,
                            readInt(rs, "ORDINAL_POSITION"),
                            rs.getString("ASC_OR_DESC"),
                            null));
        }
    }

    private ColumnTypeFamily resolveFamily(Integer jdbcType, String rawTypeName) {
        if (jdbcType != null) {
            ColumnTypeFamily family =
                    switch (jdbcType) {
                case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR,
                        Types.LONGNVARCHAR -> ColumnTypeFamily.STRING;
                case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT ->
                        ColumnTypeFamily.INTEGER;
                case Types.NUMERIC, Types.DECIMAL, Types.FLOAT, Types.REAL, Types.DOUBLE ->
                        ColumnTypeFamily.DECIMAL;
                case Types.BOOLEAN, Types.BIT -> ColumnTypeFamily.BOOLEAN;
                case Types.DATE, Types.TIME, Types.TIMESTAMP, Types.TIME_WITH_TIMEZONE,
                        Types.TIMESTAMP_WITH_TIMEZONE -> ColumnTypeFamily.DATE_TIME;
                case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB ->
                        ColumnTypeFamily.BINARY;
                default -> null;
            };
            if (family != null) {
                return family;
            }
        }
        if (rawTypeName != null && "jsonb".equalsIgnoreCase(rawTypeName)) {
            return ColumnTypeFamily.JSON;
        }
        return ColumnTypeFamily.OTHER;
    }

    private String readDefaultSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (SQLException | AbstractMethodError e) {
            return null;
        }
    }

    private Integer readInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private boolean rsBool(ResultSet rs, String columnName) {
        try {
            return rs.getBoolean(columnName);
        } catch (SQLException e) {
            return false;
        }
    }

    private String rsString(ResultSet rs, String columnName) {
        try {
            return rs.getString(columnName);
        } catch (SQLException e) {
            return null;
        }
    }

    private static final class ForeignKeyBuilder {
        private final String name;
        private final String referencedCatalogName;
        private final String referencedSchemaName;
        private final String referencedTableName;
        private final TreeMap<Integer, String> columnNames = new TreeMap<>();
        private final TreeMap<Integer, String> referencedColumnNames = new TreeMap<>();

        private ForeignKeyBuilder(
                String name,
                String referencedCatalogName,
                String referencedSchemaName,
                String referencedTableName) {
            this.name = name;
            this.referencedCatalogName = referencedCatalogName;
            this.referencedSchemaName = referencedSchemaName;
            this.referencedTableName = referencedTableName;
        }

        private int nextSequence() {
            return columnNames.size() + 1;
        }

        private void add(int sequence, String columnName, String referencedColumnName) {
            columnNames.put(sequence, columnName);
            referencedColumnNames.put(sequence, referencedColumnName);
        }

        private ForeignKeyInfo build() {
            return new ForeignKeyInfo(
                    name,
                    new ArrayList<>(columnNames.values()),
                    referencedCatalogName,
                    referencedSchemaName,
                    referencedTableName,
                    new ArrayList<>(referencedColumnNames.values()));
        }
    }

    private static final class IndexBuilder {
        private final String name;
        private final boolean unique;
        private final List<IndexColumnInfo> columns = new ArrayList<>();

        private IndexBuilder(String name, boolean unique) {
            this.name = name;
            this.unique = unique;
        }

        private void add(IndexColumnInfo column) {
            columns.add(column);
        }

        private IndexInfo build() {
            return new IndexInfo(name, unique, columns);
        }
    }
}
