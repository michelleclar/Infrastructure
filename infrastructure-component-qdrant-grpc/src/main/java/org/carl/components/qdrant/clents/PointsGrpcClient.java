package org.carl.components.qdrant.clents;

import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.VertxPointsGrpcClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcReadStream;
import java.util.function.Function;

public class PointsGrpcClient {

    private final GrpcClient client;
    private final SocketAddress address;

    public PointsGrpcClient(GrpcClient client, SocketAddress address) {
        this.client = client;
        this.address = address;
    }

    public AsyncResult<Points.PointsOperationResponse> upsert(
            Function<Points.UpsertPoints.Builder, Points.UpsertPoints> function) {
        var builder = Points.UpsertPoints.newBuilder();
        return client.request(address, VertxPointsGrpcClient.Upsert)
                .compose(
                        request -> {
                            request.end(function.apply(builder));
                            return request.response().compose(GrpcReadStream::last);
                        });
    }
}
