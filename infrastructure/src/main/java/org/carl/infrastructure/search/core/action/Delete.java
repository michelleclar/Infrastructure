package org.carl.infrastructure.search.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import java.io.IOException;

public class Delete {
    DeleteRequest.Builder builder;
    ElasticsearchClient esClient;

    public Delete(ElasticsearchClient esClient, DeleteRequest.Builder builder) {
        this.esClient = esClient;
        this.builder = builder;
    }

    public Query index(String indexName) {
        builder.index(indexName);
        return new Query(this);
    }

    public static class Query {
        Delete delete;

        public Query(Delete delete) {
            this.delete = delete;
        }

        public Executor id(String value) {
            this.delete.builder.id(value);
            return new Executor(this.delete);
        }
    }

    public static class Executor {
        Delete delete;

        public Executor(Delete get) {
            this.delete = get;
        }

        public DeleteResponse executor() {
            try {
                return this.delete.esClient.delete(this.delete.builder.build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
