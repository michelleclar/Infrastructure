package org.carl.infrastructure.persistence.utils;

import java.util.List;

/**
 * Neutral metadata snapshots for external target database introspection.
 */
public final class DatabaseMetadataSnapshot {
    private DatabaseMetadataSnapshot() {}

    public record DatabaseInfo(
            String databaseProductName,
            String databaseProductVersion,
            String defaultSchema,
            List<SchemaInfo> schemas) {
        public DatabaseInfo {
            schemas = List.copyOf(schemas == null ? List.of() : schemas);
        }
    }

    public record SchemaInfo(
            String catalogName,
            String schemaName,
            String comment,
            List<TableInfo> tables) {
        public SchemaInfo {
            tables = List.copyOf(tables == null ? List.of() : tables);
        }
    }

    public record TableInfo(
            String catalogName,
            String schemaName,
            String tableName,
            String tableType,
            String comment,
            List<ColumnInfo> columns,
            PrimaryKeyInfo primaryKey,
            List<ForeignKeyInfo> foreignKeys,
            List<IndexInfo> indexes,
            List<CheckConstraintInfo> checkConstraints) {
        public TableInfo {
            columns = List.copyOf(columns == null ? List.of() : columns);
            foreignKeys = List.copyOf(foreignKeys == null ? List.of() : foreignKeys);
            indexes = List.copyOf(indexes == null ? List.of() : indexes);
            checkConstraints = List.copyOf(checkConstraints == null ? List.of() : checkConstraints);
        }
    }

    public record ColumnInfo(
            String name,
            String comment,
            NormalizedColumnType type,
            Integer ordinalPosition,
            boolean nullable,
            boolean generated,
            boolean autoIncrement,
            String defaultValue) {}

    public record NormalizedColumnType(
            String rawTypeName,
            Integer jdbcTypeCode,
            Integer precision,
            Integer scale,
            boolean nullable,
            boolean generated,
            boolean autoIncrement,
            String defaultValue,
            ColumnTypeFamily family) {}

    public enum ColumnTypeFamily {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        DATE_TIME,
        BINARY,
        JSON,
        OTHER
    }

    public record PrimaryKeyInfo(String name, List<String> columnNames) {
        public PrimaryKeyInfo {
            columnNames = List.copyOf(columnNames == null ? List.of() : columnNames);
        }
    }

    public record ForeignKeyInfo(
            String name,
            List<String> columnNames,
            String referencedCatalogName,
            String referencedSchemaName,
            String referencedTableName,
            List<String> referencedColumnNames) {
        public ForeignKeyInfo {
            columnNames = List.copyOf(columnNames == null ? List.of() : columnNames);
            referencedColumnNames =
                    List.copyOf(referencedColumnNames == null ? List.of() : referencedColumnNames);
        }
    }

    public record IndexInfo(String name, boolean unique, List<IndexColumnInfo> columns) {
        public IndexInfo {
            columns = List.copyOf(columns == null ? List.of() : columns);
        }
    }

    public record IndexColumnInfo(
            String columnName, Integer ordinalPosition, String sortOrder, String expression) {}

    public record CheckConstraintInfo(String name, String definition) {}
}
