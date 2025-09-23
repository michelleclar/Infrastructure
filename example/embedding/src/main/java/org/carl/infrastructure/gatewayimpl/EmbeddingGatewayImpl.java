package org.carl.infrastructure.gatewayimpl;

import com.google.protobuf.ByteString;

import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.smallrye.mutiny.Uni;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.carl.components.embedding.clents.EmbeddingGrpcClient;
import org.carl.domain.gateway.IEmbeddingGateway;
import org.carl.infrastructure.ability.IGrpcClientLogAbility;
import org.carl.infrastructure.common.exception.EmbeddingException;
import org.carl.infrastructure.gatewayimpl.database.TextEmbeddingCacheDB;
import org.carl.utils.SimHashWithHanLP;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class EmbeddingGatewayImpl implements IEmbeddingGateway, IGrpcClientLogAbility {
    @Inject EmbeddingGrpcClient embeddingGrpcClient;
    @Inject TextEmbeddingCacheDB textEmbeddingCacheDB;

    @Override
    @Bulkhead(value = 2, waitingTaskQueue = 8)
    public Uni<EmbeddingOuterClass.TextVectorResponse> textToVector(String text) {

        long simHash = SimHashWithHanLP.calculateSimHash(text, false);
        return textEmbeddingCacheDB
                .getVectorWithSimHashCheck(simHash)
                .onItem()
                .transformToUni(
                        item -> {
                            if (!item.isEmpty()) {
                                List<Float> embedding =
                                        Arrays.stream(item.getFirst().getEmbedding())
                                                .map(Double::floatValue)
                                                .toList();
                                EmbeddingOuterClass.TextVectorResponse.Builder
                                        textVectorResponseBuilder =
                                                EmbeddingOuterClass.TextVectorResponse.newBuilder();
                                textVectorResponseBuilder.addAllVector(embedding);
                                return Uni.createFrom().item(textVectorResponseBuilder.build());
                            }
                            return Uni.createFrom()
                                    .completionStage(
                                            embeddingGrpcClient
                                                    .textToVector(
                                                            builder ->
                                                                    builder.setText(text).build())
                                                    .onSuccess(this::grpcClientSuccess)
                                                    .onFailure(this::grpcClientFailure)
                                                    .toCompletionStage())
                                    .onFailure()
                                    .transform(EmbeddingException::embeddingTextToVectorException)
                                    .onItem()
                                    .transformToUni(
                                            response ->
                                                    textEmbeddingCacheDB
                                                            .save(
                                                                    simHash,
                                                                    text,
                                                                    response
                                                                            .getVectorList()
                                                                            .stream()
                                                                            .map(Float::doubleValue)
                                                                            .toArray(Double[]::new))
                                                            .onItem()
                                                            .transform(__ -> response));
                        });
    }

    @Bulkhead(value = 2, waitingTaskQueue = 8)
    public Uni<EmbeddingOuterClass.FaceVectorResponse> faceToVector(byte[] face) {
        return Uni.createFrom()
                .completionStage(
                        embeddingGrpcClient
                                .faceToVector(
                                        builder ->
                                                builder.setData(ByteString.copyFrom(face)).build())
                                .onSuccess(this::grpcClientSuccess)
                                .onFailure(this::grpcClientFailure)
                                .toCompletionStage())
                .onFailure()
                .transform(EmbeddingException::embeddingFaceToVectorException);
    }

    @Override
    public String grpcClientName() {
        return "grpc.client.embedding";
    }
}
