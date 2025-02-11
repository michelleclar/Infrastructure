package org.carl.infrastructure.search.plugins.es.build;

import static org.carl.infrastructure.util.JSON.JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.core.ability.JacksonAbility;

import java.util.Arrays;
import java.util.List;

public class Query {
    ObjectNode query;
    PagePair pagePair;

    static Query Q() {
        return new Query();
    }

    public MultiMatchQuery MultiMatchQueryBuild() {
        return new MultiMatchQuery(this);
    }

    public TermQuery TermQueryBuild() {
        return new TermQuery(this);
    }

    public Query Page(Integer frome, Integer size) {
        pagePair = new PagePair(frome, size);
        return this;
    }

    record PagePair(Integer frome, Integer size) {}

    String toQuery() {
        return query.toString();
    }

    Query toQuery(ObjectNode node) {
        if (query == null) query = JSON.createObjectNode();
        query.setAll(node);
        return this;
    }
}

class TermQuery implements BaseQuery, JacksonAbility {
    TermPair termPair;
    Query q;

    public TermQuery(Query query) {
        this.q = query;
    }

    public Query build() throws JsonProcessingException {
        ObjectNode objectNode = JSON.createObjectNode();
        ObjectNode jsonNode = JSON.createObjectNode();
        jsonNode.put(termPair.field, termPair.value);
        objectNode.set(getQueryType().name().toLowerCase(), jsonNode);
        return this.q.toQuery(objectNode);
    }

    public TermPair getTermPair() {
        return termPair;
    }

    public TermQuery setTermPair(String field, String value) {
        this.termPair = new TermPair(field, value);
        return this;
    }

    @Override
    public QueryType getQueryType() {
        return QueryType.TERM;
    }

    record TermPair(String field, String value) {}
}

class MultiMatchQuery implements BaseQuery {
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
        ObjectNode jsonNode = JSON.createObjectNode();

        ObjectNode inner = JSON.createObjectNode();
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

interface BaseQuery {
    QueryType getQueryType();
}

enum QueryType {
    TERM,
    MULTI_MATCH
}
