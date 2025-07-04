package org.carl.domain.qdrant.filter.condition;

/** Select points with payload for a specified nested field */
public class NestedCondition {
    private String field;
    private Condition condition;

    public String getField() {
        return field;
    }

    public NestedCondition setField(String field) {
        this.field = field;
        return this;
    }

    public Condition getCondition() {
        return condition;
    }

    public NestedCondition setCondition(Condition condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public String toString() {
        return "{"
                + "        \"field\":\""
                + field
                + "\""
                + ",         \"condition\":"
                + condition
                + "}";
    }
}
