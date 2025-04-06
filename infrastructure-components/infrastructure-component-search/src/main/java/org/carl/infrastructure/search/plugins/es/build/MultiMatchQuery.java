package org.carl.infrastructure.search.plugins.es.build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;

public class MultiMatchQuery implements BaseQuery {
    String query;
    List<String> fields;
    Query q;

    public MultiMatchQuery(Query query) {
        this.q = query;
    }

    public String getQuery() {
        return query;
    }

    public MultiMatchQuery setQuery(String query) {
        this.query = query;
        return this;
    }

    public Query build() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        ObjectNode inner = objectMapper.createObjectNode();
        inner.put("query", query);
        fields.forEach(field -> inner.putArray("fields").add(field));
        jsonNode.set(getQueryType().name().toLowerCase(), inner);
        return this.q.toQuery(jsonNode);
    }

    public List<String> getFields() {
        return fields;
    }

    public MultiMatchQuery setFields(String... fields) {
        this.fields = Arrays.asList(fields);
        return this;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.MULTI_MATCH;
    }
}
