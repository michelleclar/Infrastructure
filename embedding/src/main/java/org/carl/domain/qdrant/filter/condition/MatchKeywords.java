package org.carl.domain.qdrant.filter.condition;

import java.util.List;

public class MatchKeywords {
    String field;
    List<String> keywords;

    public List<String> getKeywords() {
        return keywords;
    }

    public MatchKeywords setKeywords(List<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public String getField() {
        return field;
    }

    public MatchKeywords setField(String field) {
        this.field = field;
        return this;
    }
}
