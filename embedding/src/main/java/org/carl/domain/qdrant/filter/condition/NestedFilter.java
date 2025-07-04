package org.carl.domain.qdrant.filter.condition;

import org.carl.domain.qdrant.filter.Filter;

public class NestedFilter {
    private String field;
    private Filter filter;

    public Filter getFilter() {
        return filter;
    }

    public NestedFilter setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public String getField() {
        return field;
    }

    public NestedFilter setField(String field) {
        this.field = field;
        return this;
    }
}
