package org.carl.app.command;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.infrastructure.gatewayimpl.database.TextEmbeddingCacheDB;

import org.carl.infrastructure.gatewayimpl.rpc.EmbeddingClient;
import org.carl.utils.SimHashWithHanLP;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class TextVectorUpsertCmdExe {
    @Inject TextEmbeddingCacheDB textEmbeddingCacheDB;
    @Inject EmbeddingClient embeddingClient;

    public Uni<List<Float>> execute(String text) {
        long simHash = SimHashWithHanLP.calculateSimHash(text, false);
        return textEmbeddingCacheDB
                .getVectorWithSimHashCheck(simHash)
                .onItem()
                .transformToUni(
                        item -> {
                            if (item.isPresent()) {
                                List<Float> embedding =
                                        Arrays.stream(item.get().getEmbedding())
                                                .map(Double::floatValue)
                                                .toList();
                                return Uni.createFrom().item(embedding);
                            }
                            return embeddingClient
                                    .textToVector(text)
                                    .onItem()
                                    .transformToUni(
                                            response -> {
                                                return textEmbeddingCacheDB
                                                        .save(
                                                                simHash,
                                                                text,
                                                                response.getVectorList().stream()
                                                                        .map(Float::doubleValue)
                                                                        .toArray(Double[]::new))
                                                        .onItem()
                                                        .transformToUni(
                                                                __ ->
                                                                        Uni.createFrom()
                                                                                .item(
                                                                                        response
                                                                                                .getVectorList()));
                                            });
                        });
    }
}
