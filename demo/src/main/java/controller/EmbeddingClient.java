package controller;

import com.google.protobuf.ByteString;
import io.embeddingj.client.grpc.EmbeddingOuterClass;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.components.embedding.clents.EmbeddingGrpcClient;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class EmbeddingClient {
    @Inject
    EmbeddingGrpcClient embeddingGrpcClient;


    public CompletionStage<EmbeddingOuterClass.FaceVectorResponse> faceToVector(byte[] bytes) {

        return embeddingGrpcClient
                .faceToVector(
                        builder -> builder.setData(ByteString.copyFrom(bytes)).build())
                .toCompletionStage();
    }
}
