package org.carl.infrastructure.embedding;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infrastructure.embedding.clents.EmbeddingGrpcClient;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class EmbeddingGrpcClientProvider {
    @Inject Vertx vertx;

    @Produces
    @DefaultBean
    @ApplicationScoped
    public EmbeddingGrpcClient get() {
        Config config = ConfigProvider.getConfig();
        Integer port =
                config.getOptionalValue("quarkus.embedding.port", Integer.class).orElse(50051);
        String host =
                config.getOptionalValue("quarkus.embedding.host", String.class).orElse("localhost");
        GrpcClient client = GrpcClient.client(vertx);
        return new EmbeddingGrpcClient(client, SocketAddress.inetSocketAddress(port, host));
    }
}
