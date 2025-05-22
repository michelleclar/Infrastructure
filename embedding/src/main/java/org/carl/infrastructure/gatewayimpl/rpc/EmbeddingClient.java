package org.carl.infrastructure.gatewayimpl.rpc;

import com.google.protobuf.ByteString;
import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.components.embedding.clents.EmbeddingGrpcClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EmbeddingClient {
    @Inject EmbeddingGrpcClient embeddingGrpcClient;
    private final Logger logger = Logger.getLogger(EmbeddingClient.class);

    public Uni<EmbeddingOuterClass.FaceVectorResponse> faceToVector(byte[] bytes) {
        return Uni.createFrom()
                .completionStage(
                        embeddingGrpcClient
                                .faceToVector(
                                        builder ->
                                                builder.setData(ByteString.copyFrom(bytes)).build())
                                .onSuccess(this::handleSuccess)
                                .onFailure(this::handleFailure)
                                .toCompletionStage());
    }

    public Uni<EmbeddingOuterClass.TextVectorResponse> textToVector(String text) {
        return Uni.createFrom()
                .completionStage(
                        embeddingGrpcClient
                                .textToVector(builder -> builder.setText(text).build())
                                .onSuccess(this::handleSuccess)
                                .onFailure(this::handleFailure)
                                .toCompletionStage());
    }

    private void handleSuccess(Object o) {
        if (logger.isDebugEnabled()) {
            logger.debugf("grpc success: %s", o);
        }
    }

    private void handleFailure(Throwable throwable) {
        logger.errorf("grpc failure: %s", throwable.getMessage());
        if (logger.isDebugEnabled()) {
            logger.errorf(throwable, "grpc failure: %s", throwable.getMessage());
        }
    }
}
