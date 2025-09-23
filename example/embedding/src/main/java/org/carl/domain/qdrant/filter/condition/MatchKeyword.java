package org.carl.domain.qdrant.filter.condition;

public class MatchKeyword {
    String field;
    String keyword;

    public String getKeyword() {
        return keyword;
    }

    public MatchKeyword setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getField() {
        return field;
    }

    public MatchKeyword setField(String field) {
        this.field = field;
        return this;
    }

    @Override
    public String toString() {
        return "{"
                + "        \"field\":\""
                + field
                + "\""
                + ",         \"keyword\":\""
                + keyword
                + "\""
                + "}";
    }
}
