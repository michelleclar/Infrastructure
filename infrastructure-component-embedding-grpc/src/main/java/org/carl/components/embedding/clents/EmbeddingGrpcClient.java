package org.carl.components.embedding.clents;

import io.embeddingj.client.grpc.EmbeddingOuterClass;
import io.embeddingj.client.grpc.VertxEmbeddingGrpcClient;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcReadStream;
import java.util.function.Function;

public class EmbeddingGrpcClient {

    private final GrpcClient client;
    private final SocketAddress address;

    public EmbeddingGrpcClient(GrpcClient client, SocketAddress address) {
        this.client = client;
        this.address = address;
    }

    public Future<EmbeddingOuterClass.FaceVectorResponse> faceToVector(
            Function<
                            EmbeddingOuterClass.FaceVectorRequest.Builder,
                            EmbeddingOuterClass.FaceVectorRequest>
                    function) {
        var builder = EmbeddingOuterClass.FaceVectorRequest.newBuilder();
        var apply = function.apply(builder);
        return client.request(address, VertxEmbeddingGrpcClient.FaceToVector)
                .compose(
                        request -> {
                            request.end(apply);
                            return request.response().compose(GrpcReadStream::last);
                        });
    }

    public Future<EmbeddingOuterClass.TextVectorResponse> textToVector(
            Function<
                            EmbeddingOuterClass.TextVectorRequest.Builder,
                            EmbeddingOuterClass.TextVectorRequest>
                    function) {
        var builder = EmbeddingOuterClass.TextVectorRequest.newBuilder();
        var apply = function.apply(builder);
        return client.request(address, VertxEmbeddingGrpcClient.TextToVector)
                .compose(
                        request -> {
                            request.end(apply);
                            return request.response().compose(GrpcReadStream::last);
                        });
    }
}
