package org.carl.infrastructure.qdrant;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(VertxExtension.class)
class VectorClientTest {
    private QdrantGrpcClient vectorClient;

    private static boolean qdrantReachable() {
        try (var s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", 6334), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void init() {
        vectorClient =
                new QdrantGrpcClient(
                        Vertx.vertx(), SocketAddress.inetSocketAddress(6334, "localhost"));
    }

    @Test
    void get() {
        assumeTrue(qdrantReachable(), "Qdrant not reachable — skipping");
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
        assumeTrue(qdrantReachable(), "Qdrant not reachable — skipping");
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
