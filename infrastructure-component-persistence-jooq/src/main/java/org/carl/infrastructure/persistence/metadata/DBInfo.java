package org.carl.infrastructure.persistence.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据库级元数据快照。
 *
 * <p>它描述当前连接所看到的数据库产品信息、默认 schema 以及 schema 层级的结构视图。
 */
@Deprecated
public class DBInfo {
    private final String databaseProductName;
    private final String databaseProductVersion;
    private String dialect;
    private String defaultSchema;
    private final Map<String, DBSchema> schemas = new LinkedHashMap<>();

    public DBInfo(String databaseProductName, String databaseProductVersion) {
        this.databaseProductName = databaseProductName;
        this.databaseProductVersion = databaseProductVersion;
    }

    public DBInfo addSchema(DBSchema schema) {
        if (schema == null || schema.getName() == null) {
            return this;
        }

        DBSchema current = getSchema(schema.getName());
        if (current == null) {
            this.schemas.put(schema.getName(), schema);
        } else {
            if (current.getCatalogName() == null) {
                current.setCatalogName(schema.getCatalogName());
            }
            if (current.getComment() == null) {
                current.setComment(schema.getComment());
            }
            if (current.getOwner() == null) {
                current.setOwner(schema.getOwner());
            }
            schema.getTables().values().forEach(current::addTable);
        }
        return this;
    }

    public DBSchema getOrAddSchema(String schemaName) {
        DBSchema schema = getSchema(schemaName);
        if (schema != null) {
            return schema;
        }

        DBSchema created = new DBSchema(schemaName);
        this.schemas.put(schemaName, created);
        return created;
    }

    public DBSchema getSchema(String schemaName) {
        if (schemaName == null) {
            return null;
        }

        String quotedLookup = MetadataNames.quotedIdentifierValue(schemaName);
        DBSchema schema = quotedLookup == null ? null : schemas.get(quotedLookup);
        if (schema != null) {
            return schema;
        }

        return schemas.values().stream()
                .filter(candidate -> MetadataNames.matches(candidate.getName(), schemaName))
                .findFirst()
                .orElse(null);
    }

    public Map<String, DBSchema> getSchemas() {
        return Collections.unmodifiableMap(schemas);
    }

    public String getProductVersion() {
        return this.databaseProductVersion;
    }

    public String getDatabaseProductVersion() {
        return this.databaseProductVersion;
    }

    public String getDatabaseProductName() {
        return this.databaseProductName;
    }

    public String getDialect() {
        return dialect;
    }

    public DBInfo setDialect(String dialect) {
        this.dialect = dialect;
        return this;
    }

    public String getDefaultSchema() {
        return defaultSchema;
    }

    public DBInfo setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
        return this;
    }
}
