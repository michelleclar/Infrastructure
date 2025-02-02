package org.carl.infrastructure.search.core.action;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.function.Function;

/**
 * index command
 */
public class Indices {
    ElasticsearchIndicesClient indicesClient;

    public Indices(ElasticsearchIndicesClient indicesClient) {
        this.indicesClient = indicesClient;
    }

    public CreateIndexResponse create(
            Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn) {
        try {
            return indicesClient.create(fn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CreateIndexResponse create(String indexName) {

        try {
            return indicesClient.create(c -> c.index(indexName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ElasticsearchException e) {
            if (e.getMessage().contains("already exists")) {
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public DeleteIndexResponse delete(String indexName) {

        try {
            return indicesClient.delete(c -> c.index(indexName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ElasticsearchException e) {
            if (e.getMessage().contains("already exists")) {
                return null;
            }
            throw new RuntimeException(e);
        }
    }

    public static class Executor {
        Indices indices;

        public Executor(Indices indices) {
            this.indices = indices;
        }

        public CreateIndexResponse create(
                Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn) {
            try {
                return this.indices.indicesClient.create(fn);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
