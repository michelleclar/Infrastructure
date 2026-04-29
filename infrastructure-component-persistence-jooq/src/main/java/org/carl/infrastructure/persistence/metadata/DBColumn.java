package org.carl.infrastructure.persistence.metadata;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库列的元数据快照。
 *
 * <p>该模型保留 JDBC 能稳定读取到的结构字段，同时兼容历史调用方依赖的默认值解析行为。
 */
@Deprecated
public class DBColumn {
    private String name;
    private String description;
    private String type;
    private String rawDefaultValue;
    private Integer jdbcType;
    private Integer ordinalPosition;
    private Integer columnSize;
    private Integer decimalDigits;
    private boolean nullable;
    private boolean primaryKey;
    private boolean sequence;
    private boolean autoIncrement;
    private boolean generated;

    private static final String REGEX = "'([^']*)'";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return extractDefaultContent(rawDefaultValue);
    }

    public String getRawDefaultValue() {
        return rawDefaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.rawDefaultValue = defaultValue;
    }

    public void setRawDefaultValue(String rawDefaultValue) {
        this.rawDefaultValue = rawDefaultValue;
    }

    public Integer getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(Integer jdbcType) {
        this.jdbcType = jdbcType;
    }

    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    public Integer getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    public Integer getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isSequence() {
        return sequence;
    }

    public void setSequence() {
        this.sequence = true;
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

    public void setNullable() {
        this.nullable = true;
    }

    public void setPrimaryKey() {
        this.primaryKey = true;
    }

    public String getLabel() {
        if (getDescription() != null) {
            return getDescription();
        }
        return getName();
    }

    public Object extractDefaultContent(String input) {
        if (input == null) {
            return null;
        }

        String columnType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        try {
            if ("bool".equals(columnType) || "boolean".equals(columnType)) {
                return Boolean.parseBoolean(input);
            }
            if (columnType.startsWith("int")) {
                return Integer.parseInt(input);
            }
        } catch (RuntimeException ignore) {
            return input;
        }

        Matcher matcher = PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("description", description)
                .append("type", type)
                .append("rawDefaultValue", rawDefaultValue)
                .append("jdbcType", jdbcType)
                .append("ordinalPosition", ordinalPosition)
                .append("columnSize", columnSize)
                .append("decimalDigits", decimalDigits)
                .append("nullable", nullable)
                .append("primaryKey", primaryKey)
                .append("sequence", sequence)
                .append("autoIncrement", autoIncrement)
                .append("generated", generated)
                .toString();
    }
}
