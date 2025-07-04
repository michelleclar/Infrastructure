package org.carl.infrastructure.gatewayimpl.database;

import static org.carl.generated.Tables.TEXT_EMBEDDING_CACHE;

import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.generated.tables.pojos.TextEmbeddingCache;
import org.carl.generated.tables.records.TextEmbeddingCacheRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class TextEmbeddingCacheDB {
    @Inject DSLContext dslContext;

    public Uni<List<TextEmbeddingCache>> getVectorWithSimHashCheck(long simHash) {
        CompletionStage<List<TextEmbeddingCache>> vectors =
                CompletableFuture.supplyAsync(
                                () ->
                                        dslContext
                                                .select(TEXT_EMBEDDING_CACHE.EMBEDDING)
                                                .from(TEXT_EMBEDDING_CACHE)
                                                .where(
                                                        DSL.bitCount(
                                                                        TEXT_EMBEDDING_CACHE
                                                                                .SIM_HASH.bitXor(
                                                                                simHash))
                                                                .le(3))
                                                .fetch()
                                                .map(
                                                        (r) -> {
                                                            TextEmbeddingCache textEmbeddingCache =
                                                                    new TextEmbeddingCache();
                                                            textEmbeddingCache.setEmbedding(
                                                                    r.get(
                                                                            TEXT_EMBEDDING_CACHE
                                                                                    .EMBEDDING));
                                                            return textEmbeddingCache;
                                                        }))
                        .minimalCompletionStage();
        return Uni.createFrom().completionStage(vectors);
    }

    public Uni<Integer> save(long simHash, String text, Double[] embeddings) {
        TextEmbeddingCacheRecord record = new TextEmbeddingCacheRecord();
        record.setSimHash(simHash).setText(text).setEmbedding(embeddings);
        CompletionStage<Integer> integerCompletionStage =
                dslContext.insertInto(TEXT_EMBEDDING_CACHE).set(record).executeAsync();
        return Uni.createFrom().completionStage(integerCompletionStage);
    }
}
