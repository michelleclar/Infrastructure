package org.carl.infrastructure.embedding;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.vertx.core.Vertx;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.junit5.VertxExtension;

import org.carl.infrastructure.embedding.clents.EmbeddingGrpcClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.net.SocketAddress;

@ExtendWith(VertxExtension.class)
class EmbeddingGrpcClientTest {

    private static boolean embeddingReachable() {
        try (var s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress("localhost", 50051), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void embeddingClientConstructor_doesNotThrow() {
        Vertx vertx = Vertx.vertx();
        try {
            GrpcClient grpcClient = GrpcClient.client(vertx);
            EmbeddingGrpcClient client =
                    new EmbeddingGrpcClient(grpcClient, SocketAddress.inetSocketAddress(50051, "localhost"));
            assertNotNull(client);
        } finally {
            vertx.close();
        }
    }

    @Test
    void iEmbeddingAbilityMixin_getEmbeddingClient_returnsInstance() {
        Vertx vertx = Vertx.vertx();
        try {
            GrpcClient grpcClient = GrpcClient.client(vertx);
            EmbeddingGrpcClient client =
                    new EmbeddingGrpcClient(grpcClient, SocketAddress.inetSocketAddress(50051, "localhost"));
            IEmbeddingAbility ability = () -> client;
            assertNotNull(ability.getEmbeddingClient());
        } finally {
            vertx.close();
        }
    }

    @Test
    void textToVector_skipsIfNotReachable() {
        assumeTrue(embeddingReachable(), "Embedding service not reachable on localhost:50051");
        Vertx vertx = Vertx.vertx();
        try {
            GrpcClient grpcClient = GrpcClient.client(vertx);
            EmbeddingGrpcClient client =
                    new EmbeddingGrpcClient(grpcClient, SocketAddress.inetSocketAddress(50051, "localhost"));
            IEmbeddingAbility ability = () -> client;
            assertNotNull(ability.textToVector(b -> b.setText("hello").build()));
        } finally {
            vertx.close();
        }
    }
}
