package org.carl.components.vector;

import io.qdrant.client.grpc.Collections;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.concurrent.CompletionStage;
import org.carl.components.qdrant.QdrantGrpcClient;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/vector")
public class VectorClient {

    Integer port;

    String host;

    QdrantGrpcClient vectorClient;

    public VectorClient() {
        Config config = ConfigProvider.getConfig();
        port = config.getOptionalValue("quarkus.qdrant.port", Integer.class).orElse(6334);
        host = config.getOptionalValue("quarkus.qdrant.host", String.class).orElse("localhost");
        vectorClient =
                new QdrantGrpcClient(Vertx.vertx(), SocketAddress.inetSocketAddress(port, host));
    }

    @GET
    @Path("/get")
    public Uni<String> get(@QueryParam("collectionName") String collectionName) {
        CompletionStage<Collections.GetCollectionInfoResponse> completionStage =
                vectorClient.get(collectionName).toCompletionStage();
        return Uni.createFrom()
                .completionStage(completionStage)
                .onItem()
                .transform(
                        item -> {
                            System.out.println(item);
                            return item.getResult().toString();
                        });
    }

    @POST
    @Path("/create")
    public Uni<Boolean> create(@QueryParam("collectionName") String collectionName) {
        var completionStage = vectorClient.create(collectionName).toCompletionStage();
        return Uni.createFrom()
                .completionStage(completionStage)
                .onItem()
                .transform(
                        item -> {
                            System.out.println(item);
                            return item.getResult();
                        });
    }
}
