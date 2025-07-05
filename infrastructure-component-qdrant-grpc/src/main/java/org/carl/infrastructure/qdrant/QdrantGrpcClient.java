package org.carl.infrastructure.qdrant;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import org.carl.infrastructure.qdrant.clents.CollectionsGrpcClient;
import org.carl.infrastructure.qdrant.clents.PointsGrpcClient;

public class QdrantGrpcClient {
    private final io.vertx.grpc.client.GrpcClient client;
    private final SocketAddress address;
    private final CollectionsGrpcClient collectionsGrpcClient;
    private final PointsGrpcClient pointsGrpcClient;

    public QdrantGrpcClient(Vertx vertx, SocketAddress address) {
        this.client = io.vertx.grpc.client.GrpcClient.client(vertx);
        this.address = address;
        this.collectionsGrpcClient = new CollectionsGrpcClient(client, this.address);
        this.pointsGrpcClient = new PointsGrpcClient(client, this.address);
    }

    public CollectionsGrpcClient getCollectionsGrpcClient() {
        return collectionsGrpcClient;
    }

    public PointsGrpcClient getPointsGrpcClient() {
        return pointsGrpcClient;
    }

    public GrpcClient getClient() {
        return client;
    }

    public SocketAddress getAddress() {
        return address;
    }
}
