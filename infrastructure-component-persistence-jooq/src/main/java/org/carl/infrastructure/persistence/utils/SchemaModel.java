package org.carl.infrastructure.persistence.utils;

import java.util.List;

/**
 * Neutral schema model used by parser, prechecker, generator, and reverse generator.
 */
public final class SchemaModel {
    private SchemaModel() {}

    public record Database(String name, List<Schema> schemas) {
        public Database {
            schemas = List.copyOf(schemas == null ? List.of() : schemas);
        }
    }

    public record Schema(String name, String comment, List<Table> tables) {
        public Schema {
            tables = List.copyOf(tables == null ? List.of() : tables);
        }
    }

    public record Table(
            String name,
            String comment,
            List<Column> columns,
            PrimaryKey primaryKey,
            List<ForeignKey> foreignKeys,
            List<Index> indexes,
            List<CheckConstraint> checkConstraints) {
        public Table {
            columns = List.copyOf(columns == null ? List.of() : columns);
            foreignKeys = List.copyOf(foreignKeys == null ? List.of() : foreignKeys);
            indexes = List.copyOf(indexes == null ? List.of() : indexes);
            checkConstraints = List.copyOf(checkConstraints == null ? List.of() : checkConstraints);
        }
    }

    public record Column(
            String name,
            ColumnType type,
            boolean nullable,
            String defaultValue,
            boolean autoIncrement,
            boolean generated,
            String comment) {}

    public record ColumnType(String rawTypeName, Integer jdbcTypeCode, Integer precision, Integer scale) {}

    public record PrimaryKey(String name, List<String> columnNames) {
        public PrimaryKey {
            columnNames = List.copyOf(columnNames == null ? List.of() : columnNames);
        }
    }

    public record ForeignKey(
            String name,
            List<String> columnNames,
            String referencedSchemaName,
            String referencedTableName,
            List<String> referencedColumnNames) {
        public ForeignKey {
            columnNames = List.copyOf(columnNames == null ? List.of() : columnNames);
            referencedColumnNames =
                    List.copyOf(referencedColumnNames == null ? List.of() : referencedColumnNames);
        }
    }

    public record Index(String name, boolean unique, List<String> columnNames) {
        public Index {
            columnNames = List.copyOf(columnNames == null ? List.of() : columnNames);
        }
    }

    public record CheckConstraint(String name, String definition) {}
}
