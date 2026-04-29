package org.carl.infrastructure.search.plugins.es.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;

import java.io.IOException;

public class Index<T> {
    IndexRequest.Builder<T> builder;
    ElasticsearchClient esClient;

    public Index(ElasticsearchClient esClient, IndexRequest.Builder<T> builder) {
        this.esClient = esClient;
        this.builder = builder;
    }

    public Query<T> index(String indexName) {
        builder.index(indexName);
        return new Query<>(this);
    }

    public static class Query<T> {
        Index<T> index;

        public Query(Index<T> index) {
            this.index = index;
        }

        public Doc<T> id(String value) {
            this.index.builder.id(value);
            return new Doc<>(this.index);
        }
    }

    public static class Doc<T> {
        Index<T> index;

        public Doc(Index<T> index) {
            this.index = index;
        }

        public Executor<T> document(T doc) {
            this.index.builder.document(doc);
            return new Executor<>(this.index);
        }
    }

    public static class Executor<T> {
        Index<T> index;

        public Executor(Index<T> get) {
            this.index = get;
        }

        public IndexResponse executor() {
            try {
                return this.index.esClient.index(this.index.builder.build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
