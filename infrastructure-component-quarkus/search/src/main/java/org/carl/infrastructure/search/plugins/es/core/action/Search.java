package org.carl.infrastructure.search.plugins.es.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;

import org.carl.infrastructure.search.plugins.es.core.utils.ElasticsearchClientToolUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * search command
 */
public class Search {
    SearchRequest.Builder builder;
    ElasticsearchClient esClient;

    public Search(ElasticsearchClient esClient, SearchRequest.Builder builder) {
        this.esClient = esClient;
        this.builder = builder;
    }

    public _Query index(String indexName) {
        this.builder.index(indexName);
        return new _Query(this);
    }

    public static class _Query {
        Search search;

        public _Query(Search search) {
            this.search = search;
        }

        public Fetch query(Function<Query.Builder, ObjectBuilder<Query>> fn) {
            this.search.builder.query(fn);
            return new Fetch(this.search);
        }
    }

    public static class Fetch {
        Search search;

        public Fetch(Search search) {
            this.search = search;
        }

        public <T> SearchResponse<T> fetch(Class<T> clazz) {
            try {
                return this.search.esClient.search(this.search.builder.build(), clazz);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> List<T> fetchOf(Class<T> clazz) {
            try {
                SearchResponse<T> search =
                        this.search.esClient.search(this.search.builder.build(), clazz);
                return ElasticsearchClientToolUtils.hitsToList(search.hits().hits());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
