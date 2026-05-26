package org.carl.infrastructure.qdrant;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(VertxExtension.class)
class VectorClientTest {
    private Vertx vertx;
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
        vertx = Vertx.vertx();
        vectorClient = new QdrantGrpcClient(vertx, SocketAddress.inetSocketAddress(6334, "localhost"));
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    @Test
    void get(VertxTestContext testContext) {
        assumeTrue(qdrantReachable(), "Qdrant not reachable — skipping");
        vectorClient
                .getCollectionsGrpcClient()
                .get(builder -> builder.setCollectionName("test").build())
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }

    @Test
    void create(VertxTestContext testContext) {
        assumeTrue(qdrantReachable(), "Qdrant not reachable — skipping");
        vectorClient
                .getCollectionsGrpcClient()
                .create(builder -> builder.setCollectionName("test").build())
                .onSuccess(response -> testContext.completeNow())
                .onFailure(testContext::failNow);
    }
}
