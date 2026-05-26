package org.carl.infrastructure.embedding;

import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.vertx.core.Future;

import java.util.function.Function;

import org.carl.infrastructure.embedding.clents.EmbeddingGrpcClient;

public interface IEmbeddingAbility {

    EmbeddingGrpcClient getEmbeddingClient();

    default Future<EmbeddingOuterClass.TextVectorResponse> textToVector(
            Function<EmbeddingOuterClass.TextVectorRequest.Builder, EmbeddingOuterClass.TextVectorRequest> fn) {
        return getEmbeddingClient().textToVector(fn);
    }

    default Future<EmbeddingOuterClass.FaceVectorResponse> faceToVector(
            Function<EmbeddingOuterClass.FaceVectorRequest.Builder, EmbeddingOuterClass.FaceVectorRequest> fn) {
        return getEmbeddingClient().faceToVector(fn);
    }
}
