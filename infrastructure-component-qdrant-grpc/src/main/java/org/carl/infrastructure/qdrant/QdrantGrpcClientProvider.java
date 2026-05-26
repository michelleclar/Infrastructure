package org.carl.infrastructure.qdrant;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class QdrantGrpcClientProvider {

    @Inject Vertx vertx;

    @Produces
    @ApplicationScoped
    @DefaultBean
    public QdrantGrpcClient get() {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getOptionalValue("quarkus.qdrant.port", Integer.class).orElse(6334);
        String host =
                config.getOptionalValue("quarkus.qdrant.host", String.class).orElse("localhost");
        return new QdrantGrpcClient(vertx, SocketAddress.inetSocketAddress(port, host));
    }
}
