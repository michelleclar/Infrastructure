package org.carl.components.qdrant.clents;

import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.VertxPointsGrpcClient;
import io.vertx.core.Future;
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

    public Future<Points.PointsOperationResponse> upsert(
            Function<Points.UpsertPoints.Builder, Points.UpsertPoints> function) {
        var builder = Points.UpsertPoints.newBuilder();
        return client.request(address, VertxPointsGrpcClient.Upsert)
                .compose(
                        request -> {
                            request.end(function.apply(builder));
                            return request.response().compose(GrpcReadStream::last);
                        });
    }

    public Future<Points.GetResponse> get(
            Function<Points.GetPoints.Builder, Points.GetPoints> function) {
        var builder = Points.GetPoints.newBuilder();
        return client.request(address, VertxPointsGrpcClient.Get)
                .compose(
                        request -> {
                            request.end(function.apply(builder));
                            return request.response().compose(GrpcReadStream::last);
                        });
    }

    public Future<Points.QueryResponse> query(
            Function<Points.QueryPoints.Builder, Points.QueryPoints> function) {
        var builder = Points.QueryPoints.newBuilder();
        return client.request(address, VertxPointsGrpcClient.Query)
                .compose(
                        request -> {
                            request.end(function.apply(builder));
                            return request.response().compose(GrpcReadStream::last);
                        });
    }
}
