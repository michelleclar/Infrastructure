package org.carl.infrastructure.qdrant.clents;

import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.VertxCollectionsGrpcClient;
import io.vertx.core.Future;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcReadStream;
import java.util.function.Function;

public class CollectionsGrpcClient {

    private final GrpcClient client;
    private final SocketAddress address;

    public CollectionsGrpcClient(GrpcClient client, SocketAddress address) {
        this.client = client;
        this.address = address;
    }

    public Future<Collections.GetCollectionInfoResponse> get(
            Function<
                            Collections.GetCollectionInfoRequest.Builder,
                            Collections.GetCollectionInfoRequest>
                    function) {
        Collections.GetCollectionInfoRequest.Builder builder =
                Collections.GetCollectionInfoRequest.newBuilder();
        Collections.GetCollectionInfoRequest apply = function.apply(builder);
        return client.request(address, VertxCollectionsGrpcClient.Get)
                .compose(
                        request -> {
                            request.end(apply);
                            return request.response().compose(GrpcReadStream::last);
                        });
    }

    public Future<Collections.CollectionOperationResponse> create(
            Function<Collections.CreateCollection.Builder, Collections.CreateCollection> function) {
        Collections.CreateCollection.Builder builder = Collections.CreateCollection.newBuilder();
        Collections.CreateCollection apply = function.apply(builder);
        return client.request(address, VertxCollectionsGrpcClient.Create)
                .compose(
                        request -> {
                            request.end(apply);
                            return request.response().compose(GrpcReadStream::last);
                        });
    }

    public Future<Collections.CollectionExistsResponse> collectionExists(
            Function<
                            Collections.CollectionExistsRequest.Builder,
                            Collections.CollectionExistsRequest>
                    function) {
        Collections.CollectionExistsRequest.Builder builder =
                Collections.CollectionExistsRequest.newBuilder();
        Collections.CollectionExistsRequest apply = function.apply(builder);
        return client.request(address, VertxCollectionsGrpcClient.CollectionExists)
                .compose(
                        request -> {
                            request.end(apply);
                            return request.response().compose(GrpcReadStream::last);
                        });
    }
}
