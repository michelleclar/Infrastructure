package org.carl.infrastructure.search.plugins.es.build;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        if (query == null) query = new ObjectMapper().createObjectNode();
        query.setAll(node);
        return this;
    }

    public class TermQuery implements BaseQuery {
        TermPair termPair;
        Query q;

        public TermQuery(Query query) {
            this.q = query;
        }

        public Query build() throws JsonProcessingException {
            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            ObjectNode jsonNode = new ObjectMapper().createObjectNode();
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
}

interface BaseQuery {
    QueryType getQueryType();
}

enum QueryType {
    TERM,
    MULTI_MATCH
}
