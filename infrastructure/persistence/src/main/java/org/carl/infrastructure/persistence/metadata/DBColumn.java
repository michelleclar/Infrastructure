package org.carl.infrastructure.persistence.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class DBColumn {
    private String name;
    private String description;
    private String type;
    private String defaultValue;
    private boolean nullable;
    private boolean primaryKey;
    private boolean sequence;

    // 使用正则表达式来匹配单引号之间的内容
    private static String regex = "'([^']*)'";
    private static Pattern pattern = Pattern.compile(regex);

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
        return extractDefaultContent(defaultValue);
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
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

        if (this.getType().equalsIgnoreCase("bool")) {
            return Boolean.parseBoolean(input);
        } else if (this.getType().startsWith("int")) {
            return Integer.parseInt(input);
        }

        Matcher matcher = pattern.matcher(input);
        // 查找并返回匹配的内容
        if (matcher.find()) {
            return matcher.group(1); // 返回第一个括号中的匹配结果
        }
        return input; // 如果没有匹配的内容，返回 null
    }
}
