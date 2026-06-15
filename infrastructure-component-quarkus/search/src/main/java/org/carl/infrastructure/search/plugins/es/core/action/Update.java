package org.carl.infrastructure.search.plugins.es.core.action;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;

import java.io.IOException;

/**
 * update command
 *
 * @param <T> 文档类型（TDocument，用于响应反序列化）
 * @param <K> 局部更新文档类型（TPartialDocument，即 upsert 写入的文档）
 */
public class Update<T, K> {
    UpdateRequest.Builder<T, K> builder;
    ElasticsearchClient esClient;

    public Update(ElasticsearchClient esClient, UpdateRequest.Builder<T, K> builder) {
        this.esClient = esClient;
        this.builder = builder;
    }

    public Query<T, K> index(String indexName) {
        this.builder.index(indexName);
        return new Query<>(this);
    }

    public static class Query<T, K> {
        Update<T, K> update;

        public Query(Update<T, K> update) {
            this.update = update;
        }

        public Action<T, K> id(String value) {
            this.update.builder.id(value);
            return new Action<>(this.update);
        }
    }

    public static class Action<T, K> {
        Update<T, K> update;

        public Action(Update<T, K> update) {
            this.update = update;
        }

        /** 以 upsert 语义写入文档：存在则按 doc 部分更新，不存在则按 doc 插入。 */
        public Executor<T, K> upsert(K doc) {
            this.update.builder.doc(doc).docAsUpsert(true);
            return new Executor<>(this.update, doc);
        }
    }

    public static class Executor<T, K> {
        Update<T, K> update;
        K doc;

        public Executor(Update<T, K> update, K doc) {
            this.update = update;
            this.doc = doc;
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
