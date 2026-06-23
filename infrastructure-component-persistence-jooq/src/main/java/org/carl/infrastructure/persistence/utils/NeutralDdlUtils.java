package org.carl.infrastructure.persistence.utils;

import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ColumnInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.DatabaseInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.ForeignKeyInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.IndexInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.SchemaInfo;
import org.carl.infrastructure.persistence.utils.DatabaseMetadataSnapshot.TableInfo;
import org.carl.infrastructure.persistence.utils.SchemaModel.CheckConstraint;
import org.carl.infrastructure.persistence.utils.SchemaModel.Column;
import org.carl.infrastructure.persistence.utils.SchemaModel.ColumnType;
import org.carl.infrastructure.persistence.utils.SchemaModel.Database;
import org.carl.infrastructure.persistence.utils.SchemaModel.ForeignKey;
import org.carl.infrastructure.persistence.utils.SchemaModel.Index;
import org.carl.infrastructure.persistence.utils.SchemaModel.PrimaryKey;
import org.carl.infrastructure.persistence.utils.SchemaModel.Schema;
import org.carl.infrastructure.persistence.utils.SchemaModel.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parser, prechecker, generator, and reverse-generator for neutral DDL models.
 */
public final class NeutralDdlUtils {
    private NeutralDdlUtils() {}

    public static DdlParserResult parse(ExternalDatabaseKind kind, String ddl) {
        if (kind == null) {
            return DdlParserResult.failure(
                    List.of(new DdlIssue("database.kind", "Database kind is required")));
        }
        if (ddl == null || ddl.isBlank()) {
            return DdlParserResult.failure(List.of(new DdlIssue("ddl", "DDL text is required")));
        }

        Map<String, MutableSchema> schemas = new LinkedHashMap<>();
        List<DdlIssue> warnings = new ArrayList<>();
        List<DdlIssue> errors = new ArrayList<>();
        for (String statement : splitStatements(ddl)) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            try {
                if (lower.startsWith("create table")) {
                    parseCreateTable(trimmed, schemas);
                } else if (lower.startsWith("create unique index") || lower.startsWith("create index")) {
                    parseCreateIndex(trimmed, schemas);
                } else if (lower.startsWith("comment on table")) {
                    parseTableComment(trimmed, schemas);
                } else if (lower.startsWith("comment on column")) {
                    parseColumnComment(trimmed, schemas);
                } else {
                    warnings.add(new DdlIssue("ddl", "Unsupported statement ignored: " + trimmed));
                }
            } catch (RuntimeException e) {
                errors.add(new DdlIssue("ddl", e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            return DdlParserResult.failure(errors);
        }
        Database database =
                new Database(
                        null,
                        schemas.values().stream().map(MutableSchema::toSchema).toList());
        return DdlParserResult.success(database, warnings);
    }

    public static DdlPrecheckResult precheck(Database database) {
        List<DdlIssue> errors = new ArrayList<>();
        if (database == null) {
            return DdlPrecheckResult.failure(
                    List.of(new DdlIssue("database", "Database model is required")));
        }
        for (Schema schema : database.schemas()) {
            String schemaPath = "schemas[" + valuePath(schema.name()) + "]";
            if (isBlank(schema.name())) {
                errors.add(new DdlIssue(schemaPath, "Schema name is required"));
            }
            for (Table table : schema.tables()) {
                String tablePath = schemaPath + ".tables[" + valuePath(table.name()) + "]";
                if (isBlank(table.name())) {
                    errors.add(new DdlIssue(tablePath, "Table name is required"));
                }
                Set<String> columnNames = new LinkedHashSet<>();
                for (Column column : table.columns()) {
                    String columnPath = tablePath + ".columns[" + valuePath(column.name()) + "]";
                    if (isBlank(column.name())) {
                        errors.add(new DdlIssue(columnPath, "Column name is required"));
                    } else if (!columnNames.add(column.name())) {
                        errors.add(new DdlIssue(columnPath, "Duplicate column name: " + column.name()));
                    }
                    if (column.type() == null || isBlank(column.type().rawTypeName())) {
                        errors.add(new DdlIssue(columnPath + ".type", "Column type is required"));
                    }
                }
                validatePrimaryKey(table.primaryKey(), columnNames, tablePath, errors);
                validateForeignKeys(table.foreignKeys(), columnNames, tablePath, errors);
                validateIndexes(table.indexes(), columnNames, tablePath, errors);
                validateCheckConstraints(table.checkConstraints(), tablePath, errors);
            }
        }
        if (!errors.isEmpty()) {
            return DdlPrecheckResult.failure(errors);
        }
        return DdlPrecheckResult.success(List.of());
    }

    public static DdlGeneratorResult generate(ExternalDatabaseKind kind, Database database) {
        if (kind == null) {
            return DdlGeneratorResult.failure(
                    List.of(new DdlIssue("database.kind", "Database kind is required")));
        }
        DdlPrecheckResult precheck = precheck(database);
        if (!precheck.success()) {
            return DdlGeneratorResult.failure(precheck.errors());
        }
        StringBuilder ddl = new StringBuilder();
        for (Schema schema : database.schemas()) {
            for (Table table : schema.tables()) {
                appendCreateTable(kind, ddl, schema, table);
                appendComments(kind, ddl, schema, table);
                appendIndexes(kind, ddl, schema, table);
            }
        }
        return DdlGeneratorResult.success(ddl.toString(), precheck.warnings());
    }

    public static DdlReverseGeneratorResult reverseGenerate(
            ExternalDatabaseKind kind, DatabaseInfo metadata) {
        if (metadata == null) {
            return DdlReverseGeneratorResult.failure(
                    null, List.of(new DdlIssue("metadata", "Metadata snapshot is required")));
        }
        Database database = fromMetadata(metadata);
        DdlGeneratorResult generated = generate(kind, database);
        if (!generated.success()) {
            return DdlReverseGeneratorResult.failure(database, generated.errors());
        }
        return DdlReverseGeneratorResult.success(database, generated.ddl(), generated.warnings());
    }

    public static Database fromMetadata(DatabaseInfo metadata) {
        List<Schema> schemas = new ArrayList<>();
        for (SchemaInfo schema : metadata.schemas()) {
            List<Table> tables = new ArrayList<>();
            for (TableInfo table : schema.tables()) {
                tables.add(fromMetadata(table));
            }
            schemas.add(new Schema(schema.schemaName(), schema.comment(), tables));
        }
        return new Database(metadata.databaseProductName(), schemas);
    }

    public static Table fromMetadata(TableInfo table) {
        List<Column> columns = new ArrayList<>();
        for (ColumnInfo column : table.columns()) {
            columns.add(
                    new Column(
                            column.name(),
                            new ColumnType(
                                    column.type().rawTypeName(),
                                    column.type().jdbcTypeCode(),
                                    column.type().precision(),
                                    column.type().scale()),
                            column.nullable(),
                            column.defaultValue(),
                            column.autoIncrement(),
                            column.generated(),
                            column.comment()));
        }
        PrimaryKey primaryKey =
                table.primaryKey() == null
                        ? null
                        : new PrimaryKey(
                                table.primaryKey().name(), table.primaryKey().columnNames());
        List<ForeignKey> foreignKeys = new ArrayList<>();
        for (ForeignKeyInfo foreignKey : table.foreignKeys()) {
            foreignKeys.add(
                    new ForeignKey(
                            foreignKey.name(),
                            foreignKey.columnNames(),
                            foreignKey.referencedSchemaName(),
                            foreignKey.referencedTableName(),
                            foreignKey.referencedColumnNames()));
        }
        List<Index> indexes = new ArrayList<>();
        for (IndexInfo index : table.indexes()) {
            indexes.add(
                    new Index(
                            index.name(),
                            index.unique(),
                            index.columns().stream()
                                    .map(DatabaseMetadataSnapshot.IndexColumnInfo::columnName)
                                    .toList()));
        }
        List<CheckConstraint> checks =
                table.checkConstraints().stream()
                        .map(check -> new CheckConstraint(check.name(), check.definition()))
                        .toList();
        return new Table(
                table.tableName(),
                table.comment(),
                columns,
                primaryKey,
                foreignKeys,
                indexes,
                checks);
    }

    private static void validatePrimaryKey(
            PrimaryKey primaryKey, Set<String> columnNames, String tablePath, List<DdlIssue> errors) {
        if (primaryKey == null) {
            return;
        }
        for (String columnName : primaryKey.columnNames()) {
            if (!columnNames.contains(columnName)) {
                errors.add(
                        new DdlIssue(
                                tablePath + ".primaryKey.columns[" + valuePath(columnName) + "]",
                                "Primary key column does not exist: " + columnName));
            }
        }
    }

    private static void validateForeignKeys(
            List<ForeignKey> foreignKeys,
            Set<String> columnNames,
            String tablePath,
            List<DdlIssue> errors) {
        for (ForeignKey foreignKey : foreignKeys) {
            String path = tablePath + ".foreignKeys[" + valuePath(foreignKey.name()) + "]";
            if (isBlank(foreignKey.referencedTableName())) {
                errors.add(new DdlIssue(path + ".referencedTableName", "Referenced table is required"));
            }
            if (foreignKey.columnNames().size() != foreignKey.referencedColumnNames().size()) {
                errors.add(new DdlIssue(path, "Foreign key column count must match referenced column count"));
            }
            for (String columnName : foreignKey.columnNames()) {
                if (!columnNames.contains(columnName)) {
                    errors.add(
                            new DdlIssue(
                                    path + ".columns[" + valuePath(columnName) + "]",
                                    "Foreign key column does not exist: " + columnName));
                }
            }
        }
    }

    private static void validateIndexes(
            List<Index> indexes, Set<String> columnNames, String tablePath, List<DdlIssue> errors) {
        for (Index index : indexes) {
            String path = tablePath + ".indexes[" + valuePath(index.name()) + "]";
            if (isBlank(index.name())) {
                errors.add(new DdlIssue(path, "Index name is required"));
            }
            for (String columnName : index.columnNames()) {
                if (!columnNames.contains(columnName)) {
                    errors.add(
                            new DdlIssue(
                                    path + ".columns[" + valuePath(columnName) + "]",
                                    "Index column does not exist: " + columnName));
                }
            }
        }
    }

    private static void validateCheckConstraints(
            List<CheckConstraint> checks, String tablePath, List<DdlIssue> errors) {
        for (CheckConstraint check : checks) {
            String path = tablePath + ".checkConstraints[" + valuePath(check.name()) + "]";
            if (isBlank(check.definition())) {
                errors.add(new DdlIssue(path, "Check constraint definition is required"));
            }
        }
    }

    private static void appendCreateTable(
            ExternalDatabaseKind kind, StringBuilder ddl, Schema schema, Table table) {
        ddl.append("create table ")
                .append(qualify(kind, schema.name(), table.name()))
                .append(" (\n");
        List<String> lines = new ArrayList<>();
        for (Column column : table.columns()) {
            lines.add("    " + renderColumn(kind, column));
        }
        if (table.primaryKey() != null && !table.primaryKey().columnNames().isEmpty()) {
            String prefix =
                    isBlank(table.primaryKey().name())
                            ? "primary key"
                            : "constraint "
                                    + quote(kind, table.primaryKey().name())
                                    + " primary key";
            lines.add(prefix + " (" + joinQuoted(kind, table.primaryKey().columnNames()) + ")");
        }
        for (ForeignKey foreignKey : table.foreignKeys()) {
            String referencedTable =
                    qualify(kind, foreignKey.referencedSchemaName(), foreignKey.referencedTableName());
            String prefix =
                    isBlank(foreignKey.name())
                            ? "foreign key"
                            : "constraint " + quote(kind, foreignKey.name()) + " foreign key";
            lines.add(
                    prefix
                            + " ("
                            + joinQuoted(kind, foreignKey.columnNames())
                            + ") references "
                            + referencedTable
                            + " ("
                            + joinQuoted(kind, foreignKey.referencedColumnNames())
                            + ")");
        }
        for (CheckConstraint check : table.checkConstraints()) {
            String prefix =
                    isBlank(check.name())
                            ? ""
                            : "constraint " + quote(kind, check.name()) + " ";
            lines.add(prefix + renderCheckDefinition(check.definition()));
        }
        ddl.append(String.join(",\n", lines));
        ddl.append("\n)");
        if (kind == ExternalDatabaseKind.MYSQL && !isBlank(table.comment())) {
            ddl.append(" comment=").append(sqlString(table.comment()));
        }
        ddl.append(";\n");
    }

    private static String renderColumn(ExternalDatabaseKind kind, Column column) {
        StringBuilder rendered = new StringBuilder();
        rendered.append(quote(kind, column.name())).append(" ").append(renderType(column.type()));
        if (!column.nullable()) {
            rendered.append(" not null");
        }
        if (!isBlank(column.defaultValue())) {
            rendered.append(" default ").append(column.defaultValue());
        }
        if (column.autoIncrement()) {
            if (kind == ExternalDatabaseKind.MYSQL) {
                rendered.append(" auto_increment");
            } else {
                rendered.append(" generated by default as identity");
            }
        }
        if (kind == ExternalDatabaseKind.MYSQL && !isBlank(column.comment())) {
            rendered.append(" comment ").append(sqlString(column.comment()));
        }
        return rendered.toString();
    }

    private static String renderType(ColumnType type) {
        String rawType = type.rawTypeName();
        if (type.precision() == null || rawType.contains("(")) {
            return rawType;
        }
        if (type.scale() == null) {
            return rawType + "(" + type.precision() + ")";
        }
        return rawType + "(" + type.precision() + "," + type.scale() + ")";
    }

    private static String renderCheckDefinition(String definition) {
        String trimmed = definition.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("check")) {
            return trimmed;
        }
        return "check (" + trimmed + ")";
    }

    private static void appendComments(
            ExternalDatabaseKind kind, StringBuilder ddl, Schema schema, Table table) {
        if (kind != ExternalDatabaseKind.POSTGRESQL) {
            return;
        }
        if (!isBlank(table.comment())) {
            ddl.append("comment on table ")
                    .append(qualify(kind, schema.name(), table.name()))
                    .append(" is ")
                    .append(sqlString(table.comment()))
                    .append(";\n");
        }
        for (Column column : table.columns()) {
            if (isBlank(column.comment())) {
                continue;
            }
            ddl.append("comment on column ")
                    .append(qualify(kind, schema.name(), table.name()))
                    .append(".")
                    .append(quote(kind, column.name()))
                    .append(" is ")
                    .append(sqlString(column.comment()))
                    .append(";\n");
        }
    }

    private static void appendIndexes(
            ExternalDatabaseKind kind, StringBuilder ddl, Schema schema, Table table) {
        for (Index index : table.indexes()) {
            ddl.append("create ");
            if (index.unique()) {
                ddl.append("unique ");
            }
            ddl.append("index ")
                    .append(quote(kind, index.name()))
                    .append(" on ")
                    .append(qualify(kind, schema.name(), table.name()))
                    .append(" (")
                    .append(joinQuoted(kind, index.columnNames()))
                    .append(");\n");
        }
    }

    private static void parseCreateTable(String statement, Map<String, MutableSchema> schemas) {
        int open = statement.indexOf('(');
        int close = statement.lastIndexOf(')');
        if (open < 0 || close <= open) {
            throw new IllegalArgumentException("Invalid CREATE TABLE statement: " + statement);
        }
        String namePart =
                statement.substring("create table".length(), open)
                        .replaceFirst("(?i)^\\s*if\\s+not\\s+exists\\s+", "")
                        .trim();
        QualifiedName tableName = parseQualifiedName(namePart);
        MutableSchema schema = schemas.computeIfAbsent(tableName.schemaName(), MutableSchema::new);
        MutableTable table = new MutableTable(tableName.objectName());
        schema.tables.put(table.name, table);

        for (String part : splitTopLevel(statement.substring(open + 1, close), ',')) {
            String text = part.trim();
            if (text.isEmpty()) {
                continue;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.startsWith("constraint ")) {
                parseNamedConstraint(text, table);
            } else if (lower.startsWith("primary key")) {
                table.primaryKey = new PrimaryKey(null, parseColumnsInsideParentheses(text));
            } else if (lower.startsWith("foreign key")) {
                table.foreignKeys.add(parseForeignKey(null, text));
            } else if (lower.startsWith("check")) {
                table.checkConstraints.add(new CheckConstraint(null, insideParentheses(text)));
            } else {
                Column column = parseColumn(text);
                table.columns.add(column);
                if (lower.contains(" primary key")) {
                    table.primaryKey = new PrimaryKey(null, List.of(column.name()));
                }
            }
        }
    }

    private static void parseNamedConstraint(String text, MutableTable table) {
        List<String> tokens = splitWhitespaceTopLevel(text);
        if (tokens.size() < 3) {
            throw new IllegalArgumentException("Invalid constraint definition: " + text);
        }
        String name = unquote(tokens.get(1));
        String rest = text.substring(text.indexOf(tokens.get(2))).trim();
        String lowerRest = rest.toLowerCase(Locale.ROOT);
        if (lowerRest.startsWith("primary key")) {
            table.primaryKey = new PrimaryKey(name, parseColumnsInsideParentheses(rest));
        } else if (lowerRest.startsWith("foreign key")) {
            table.foreignKeys.add(parseForeignKey(name, rest));
        } else if (lowerRest.startsWith("check")) {
            table.checkConstraints.add(new CheckConstraint(name, insideParentheses(rest)));
        }
    }

    private static Column parseColumn(String text) {
        List<String> tokens = splitWhitespaceTopLevel(text);
        if (tokens.size() < 2) {
            throw new IllegalArgumentException("Invalid column definition: " + text);
        }
        String name = unquote(tokens.get(0));
        int index = 1;
        List<String> typeParts = new ArrayList<>();
        while (index < tokens.size() && !isColumnKeyword(tokens.get(index))) {
            typeParts.add(tokens.get(index));
            index++;
        }
        boolean nullable = true;
        boolean autoIncrement = false;
        boolean generated = false;
        String defaultValue = null;
        String comment = null;
        boolean inlinePrimaryKey = false;
        while (index < tokens.size()) {
            String token = tokens.get(index);
            String lower = token.toLowerCase(Locale.ROOT);
            if ("not".equals(lower) && index + 1 < tokens.size()
                    && "null".equals(tokens.get(index + 1).toLowerCase(Locale.ROOT))) {
                nullable = false;
                index += 2;
            } else if ("null".equals(lower)) {
                nullable = true;
                index++;
            } else if ("default".equals(lower) && index + 1 < tokens.size()) {
                defaultValue = tokens.get(index + 1);
                index += 2;
            } else if ("primary".equals(lower) && index + 1 < tokens.size()
                    && "key".equals(tokens.get(index + 1).toLowerCase(Locale.ROOT))) {
                inlinePrimaryKey = true;
                nullable = false;
                index += 2;
            } else if ("auto_increment".equals(lower) || "serial".equals(lower)) {
                autoIncrement = true;
                index++;
            } else if ("generated".equals(lower)) {
                generated = true;
                index++;
            } else if ("comment".equals(lower) && index + 1 < tokens.size()) {
                comment = unquoteSqlString(tokens.get(index + 1));
                index += 2;
            } else {
                index++;
            }
        }
        Column column =
                new Column(
                        name,
                        new ColumnType(String.join(" ", typeParts), null, null, null),
                        nullable,
                        defaultValue,
                        autoIncrement,
                        generated,
                        comment);
        if (inlinePrimaryKey) {
            // Inline primary keys are handled by parseCreateTable after the column is added.
        }
        return column;
    }

    private static ForeignKey parseForeignKey(String name, String text) {
        List<String> localColumns = parseColumnsInsideParentheses(text);
        int referencesIndex = text.toLowerCase(Locale.ROOT).indexOf(" references ");
        if (referencesIndex < 0) {
            throw new IllegalArgumentException("Invalid foreign key definition: " + text);
        }
        String references = text.substring(referencesIndex + " references ".length()).trim();
        int open = references.indexOf('(');
        QualifiedName referencedTable = parseQualifiedName(references.substring(0, open).trim());
        List<String> referencedColumns = parseColumnsInsideParentheses(references);
        return new ForeignKey(
                name,
                localColumns,
                referencedTable.schemaName(),
                referencedTable.objectName(),
                referencedColumns);
    }

    private static void parseCreateIndex(String statement, Map<String, MutableSchema> schemas) {
        String lower = statement.toLowerCase(Locale.ROOT);
        boolean unique = lower.startsWith("create unique index");
        String marker = " on ";
        int onIndex = lower.indexOf(marker);
        if (onIndex < 0) {
            throw new IllegalArgumentException("Invalid CREATE INDEX statement: " + statement);
        }
        String indexName =
                statement.substring(unique ? "create unique index".length() : "create index".length(), onIndex)
                        .trim();
        String target = statement.substring(onIndex + marker.length()).trim();
        int open = target.indexOf('(');
        QualifiedName tableName = parseQualifiedName(target.substring(0, open).trim());
        MutableSchema schema = schemas.computeIfAbsent(tableName.schemaName(), MutableSchema::new);
        MutableTable table = schema.tables.computeIfAbsent(tableName.objectName(), MutableTable::new);
        table.indexes.add(
                new Index(unquote(indexName), unique, parseColumnsInsideParentheses(target)));
    }

    private static void parseTableComment(String statement, Map<String, MutableSchema> schemas) {
        String marker = " is ";
        int markerIndex = statement.toLowerCase(Locale.ROOT).indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalArgumentException("Invalid table comment: " + statement);
        }
        String tablePart = statement.substring("comment on table".length(), markerIndex).trim();
        QualifiedName tableName = parseQualifiedName(tablePart);
        MutableSchema schema = schemas.computeIfAbsent(tableName.schemaName(), MutableSchema::new);
        MutableTable table = schema.tables.computeIfAbsent(tableName.objectName(), MutableTable::new);
        table.comment = unquoteSqlString(statement.substring(markerIndex + marker.length()).trim());
    }

    private static void parseColumnComment(String statement, Map<String, MutableSchema> schemas) {
        String marker = " is ";
        int markerIndex = statement.toLowerCase(Locale.ROOT).indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalArgumentException("Invalid column comment: " + statement);
        }
        String columnPart = statement.substring("comment on column".length(), markerIndex).trim();
        QualifiedName columnName = parseQualifiedName(columnPart);
        MutableSchema schema = schemas.computeIfAbsent(columnName.schemaName(), MutableSchema::new);
        MutableTable table = schema.tables.computeIfAbsent(columnName.parentName(), MutableTable::new);
        String comment = unquoteSqlString(statement.substring(markerIndex + marker.length()).trim());
        table.columnComments.put(columnName.objectName(), comment);
    }

    private static List<String> splitStatements(String ddl) {
        return splitTopLevel(ddl, ';');
    }

    private static List<String> splitTopLevel(String value, char separator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            } else if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                }
            }
            if (ch == separator && depth == 0 && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static List<String> splitWhitespaceTopLevel(String value) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            } else if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                }
            }
            if (Character.isWhitespace(ch) && depth == 0 && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static List<String> parseColumnsInsideParentheses(String text) {
        return splitTopLevel(insideParentheses(text), ',').stream()
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(NeutralDdlUtils::unquote)
                .toList();
    }

    private static String insideParentheses(String text) {
        int open = text.indexOf('(');
        int close = text.lastIndexOf(')');
        if (open < 0 || close <= open) {
            throw new IllegalArgumentException("Expected parentheses in: " + text);
        }
        return text.substring(open + 1, close).trim();
    }

    private static QualifiedName parseQualifiedName(String name) {
        List<String> parts = splitTopLevel(name, '.').stream().map(String::trim).toList();
        if (parts.size() == 1) {
            return new QualifiedName(null, null, unquote(parts.get(0)));
        }
        if (parts.size() == 2) {
            return new QualifiedName(unquote(parts.get(0)), null, unquote(parts.get(1)));
        }
        if (parts.size() == 3) {
            return new QualifiedName(unquote(parts.get(0)), unquote(parts.get(1)), unquote(parts.get(2)));
        }
        throw new IllegalArgumentException("Invalid qualified name: " + name);
    }

    private static boolean isColumnKeyword(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return "not".equals(lower)
                || "null".equals(lower)
                || "default".equals(lower)
                || "primary".equals(lower)
                || "comment".equals(lower)
                || "auto_increment".equals(lower)
                || "generated".equals(lower);
    }

    private static String quote(ExternalDatabaseKind kind, String identifier) {
        if (kind == ExternalDatabaseKind.MYSQL) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String qualify(ExternalDatabaseKind kind, String schemaName, String objectName) {
        if (isBlank(schemaName)) {
            return quote(kind, objectName);
        }
        return quote(kind, schemaName) + "." + quote(kind, objectName);
    }

    private static String joinQuoted(ExternalDatabaseKind kind, List<String> names) {
        return names.stream().map(name -> quote(kind, name)).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String unquoteSqlString(String value) {
        String trimmed = value.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private static String unquote(String identifier) {
        String trimmed = identifier.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("`") && trimmed.endsWith("`"))) {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\"\"", "\"")
                    .replace("``", "`");
        }
        return trimmed;
    }

    private static String valuePath(String value) {
        return value == null ? "<null>" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record QualifiedName(String schemaName, String parentName, String objectName) {}

    private static final class MutableSchema {
        private final String name;
        private final Map<String, MutableTable> tables = new LinkedHashMap<>();

        private MutableSchema(String name) {
            this.name = name;
        }

        private Schema toSchema() {
            return new Schema(name, null, tables.values().stream().map(MutableTable::toTable).toList());
        }
    }

    private static final class MutableTable {
        private final String name;
        private String comment;
        private final List<Column> columns = new ArrayList<>();
        private final Map<String, String> columnComments = new LinkedHashMap<>();
        private PrimaryKey primaryKey;
        private final List<ForeignKey> foreignKeys = new ArrayList<>();
        private final List<Index> indexes = new ArrayList<>();
        private final List<CheckConstraint> checkConstraints = new ArrayList<>();

        private MutableTable(String name) {
            this.name = name;
        }

        private Table toTable() {
            List<Column> resolvedColumns =
                    columns.stream()
                            .map(
                                    column ->
                                            new Column(
                                                    column.name(),
                                                    column.type(),
                                                    column.nullable(),
                                                    column.defaultValue(),
                                                    column.autoIncrement(),
                                                    column.generated(),
                                                    column.comment() == null
                                                            ? columnComments.get(column.name())
                                                            : column.comment()))
                            .toList();
            return new Table(
                    name,
                    comment,
                    resolvedColumns,
                    primaryKey,
                    foreignKeys,
                    indexes,
                    checkConstraints);
        }
    }
}
