package org.carl.infrastructure.search.plugins.es.build;

import static org.carl.infrastructure.util.JSON.JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.carl.infrastructure.ability.JacksonAbility;

public class Query {
    public ObjectNode query;
    public PagePair pagePair;

    public static Query Q() {
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

    public record PagePair(Integer frome, Integer size) {}

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

interface BaseQuery {
    QueryType getQueryType();
}

enum QueryType {
    TERM,
    MULTI_MATCH
}
