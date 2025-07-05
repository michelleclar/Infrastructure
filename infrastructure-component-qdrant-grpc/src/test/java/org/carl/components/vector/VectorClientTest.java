package org.carl.components.vector;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.carl.infrastructure.qdrant.QdrantGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VectorClientTest {
    private QdrantGrpcClient vectorClient;

    @BeforeEach
    void init() {
        vectorClient =
                new QdrantGrpcClient(
                        Vertx.vertx(), SocketAddress.inetSocketAddress(6334, "localhost"));
    }

    @Test
    void get() {
        var r =
                vectorClient
                        .getCollectionsGrpcClient()
                        .get(
                                builder -> {
                                    return builder.setCollectionName("test").build();
                                });
        r.onComplete(
                handle -> {
                    var _r = handle.result();
                    System.out.println(_r);
                });
    }

    @Test
    void create(VertxTestContext testContext) {

        vectorClient
                .getCollectionsGrpcClient()
                .create(builder -> builder.setCollectionName("test").build())
                .onSuccess(
                        response -> {
                            System.out.println(response);
                            testContext.completeNow();
                        })
                .onFailure(testContext::failNow);
    }
}
