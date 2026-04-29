package org.carl.infrastructure.persistence.metadata;

/**
 * 索引中单个列位的快照定义。
 *
 * <p>当索引列不是普通字段而是表达式时，{@code name} 可能为空，此时通过 {@code expression} 表示。
 */
@Deprecated
public class DBIndexColumn {
    private final String name;
    private final Integer ordinalPosition;
    private final String sortOrder;
    private final String expression;
    private final boolean included;

    public DBIndexColumn(String name, Integer ordinalPosition, String sortOrder) {
        this(name, ordinalPosition, sortOrder, null, false);
    }

    public DBIndexColumn(
            String name,
            Integer ordinalPosition,
            String sortOrder,
            String expression,
            boolean included) {
        this.name = name;
        this.ordinalPosition = ordinalPosition;
        this.sortOrder = sortOrder;
        this.expression = expression;
        this.included = included;
    }

    public String getName() {
        return name;
    }

    public String getReferenceName() {
        if (name != null) {
            return name;
        }
        return expression;
    }

    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isIncluded() {
        return included;
    }
}
