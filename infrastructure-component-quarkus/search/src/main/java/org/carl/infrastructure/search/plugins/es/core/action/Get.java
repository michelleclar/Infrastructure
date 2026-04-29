package org.carl.infrastructure.search.plugins.es.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;

import java.io.IOException;

public class Get {
    GetRequest.Builder builder;
    ElasticsearchClient esClient;

    public Get(ElasticsearchClient esClient, GetRequest.Builder builder) {
        this.esClient = esClient;
        this.builder = builder;
    }

    public Query index(String indexName) {
        builder.index(indexName);
        return new Query(this);
    }

    public static class Query {
        Get get;

        public Query(Get get) {
            this.get = get;
        }

        public Fetch id(String value) {
            this.get.builder.id(value);
            return new Fetch(this.get);
        }
    }

    public static class Fetch {
        Get get;

        public Fetch(Get get) {
            this.get = get;
        }

        public <T> T fetchOf(Class<T> clazz) {
            try {
                GetResponse<T> response = this.get.esClient.get(this.get.builder.build(), clazz);
                return response.source();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
