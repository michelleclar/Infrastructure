package org.carl.infrastructure.search.plugins.es.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * update command
 *
 * @param <T>
 * @param <K>
 */
public class Update<T, K> {
    UpdateRequest.Builder<T, K> builder;
    ElasticsearchClient esClient;
    T document;
    K tPartialDocument;

    public Update(
            ElasticsearchClient esClient, UpdateRequest.Builder<T, K> builder, @Nullable T doc) {
        this.esClient = esClient;
        this.builder = builder;
        this.document = doc;
    }

    public Update(
            ElasticsearchClient esClient,
            UpdateRequest.Builder<T, K> builder,
            @Nullable T doc,
            @Nullable K tPartialDocument) {
        this.esClient = esClient;
        this.builder = builder;
        this.document = doc;
        this.tPartialDocument = tPartialDocument;
    }

    public Query<T, K> index(String indexName) {
        this.builder.index(indexName);
        return new Query<>(this, document);
    }

    public static class Query<T, K> {
        Update<T, K> update;
        T doc;

        public Query(Update<T, K> update, T t) {
            this.update = update;
            this.doc = t;
        }

        public Action<T, K> id(String value) {
            this.update.builder.id(value);
            return new Action<>(this.update, doc);
        }
    }

    public static class Action<T, K> {
        Update<T, K> update;
        T doc;

        public Action(Update<T, K> update, T t) {
            this.update = update;
            this.doc = t;
        }

        public Executor<T, K> upsert() {
            return new Executor<>(this.update, doc);
        }
    }

    public static class Executor<T, K> {
        Update<T, K> update;
        T doc;

        public Executor(Update<T, K> update, T t) {
            this.update = update;
            this.doc = t;
        }

        public UpdateResponse<T> executor() {
            try {
                return update.esClient.update(this.update.builder.build(), doc.getClass());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
