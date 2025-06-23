package org.carl.components.embedding;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.carl.components.embedding.clents.EmbeddingGrpcClient;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class EmbeddingGrpcClientProvider implements Provider<EmbeddingGrpcClient> {
    @Inject Vertx vertx;

    @ApplicationScoped
    @DefaultBean
    @Override
    public EmbeddingGrpcClient get() {

        Config config = ConfigProvider.getConfig();
        Integer port =
                config.getOptionalValue("quarkus.embedding.port", Integer.class).orElse(50051);
        String host =
                config.getOptionalValue("quarkus.embedding.host", String.class).orElse("localhost");

//        GrpcClient client = GrpcClient.client(Vertx.vertx());
        GrpcClient client = GrpcClient.client(vertx);
        return new EmbeddingGrpcClient(client, SocketAddress.inetSocketAddress(port, host));
    }
}
