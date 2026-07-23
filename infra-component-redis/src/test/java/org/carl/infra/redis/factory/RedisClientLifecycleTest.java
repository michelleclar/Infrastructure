package org.carl.infra.redis.factory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class RedisClientLifecycleTest {

    @Test
    void ownedVertxIsClosedWithClient() {
        RedisClient client = RedisClientFactory.create();
        Vertx ownedVertx = client.vertxInstance();
        assertTrue(client.ownsVertxInstance());

        client.closeAsync().join();

        assertTrue(((VertxInternal) ownedVertx).closeFuture().isClosed());
    }

    @Test
    void injectedVertxRemainsOpen() {
        Vertx sharedVertx = Vertx.vertx();
        RedisClient client = RedisClientFactory.create(sharedVertx);
        assertFalse(client.ownsVertxInstance());

        client.closeAsync().join();

        assertFalse(((VertxInternal) sharedVertx).closeFuture().isClosed());
        sharedVertx.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    void commandTimeoutCompletesStalledRequest() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            CompletableFuture<Socket> accepted = new CompletableFuture<>();
            Thread serverThread =
                    Thread.ofPlatform()
                            .start(
                                    () -> {
                                        try {
                                            accepted.complete(server.accept());
                                        } catch (Exception exception) {
                                            accepted.completeExceptionally(exception);
                                        }
                                    });

            RedisConfigOptions options =
                    new RedisConfigOptions()
                            .setConnectionString("redis://127.0.0.1:" + server.getLocalPort())
                            .setConnectTimeout(1_000)
                            .setCommandTimeout(Duration.ofMillis(150));

            try (RedisClient client = RedisClientFactory.create(options)) {
                CompletionException failure =
                        assertThrows(CompletionException.class, () -> client.get("key").join());
                assertInstanceOf(TimeoutException.class, failure.getCause());
            } finally {
                Socket socket = accepted.get(2, TimeUnit.SECONDS);
                socket.close();
                serverThread.join(2_000);
            }
        }
    }

    @Test
    void invalidExpirationIsRejectedBeforeNetworkAccess() {
        try (RedisClient client = RedisClientFactory.create()) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> client.set("key", "value", Duration.ZERO));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> client.getOrSet("key", "value", Duration.ofNanos(1)));
        }
    }

    @Test
    void clusterWideScanIsRejectedInsteadOfReturningPartialResults() {
        RedisConfigOptions options =
                new RedisConfigOptions().setConnectType(SentinelType.CLUSTER);
        try (RedisClient client = RedisClientFactory.create(options)) {
            CompletionException failure =
                    assertThrows(
                            CompletionException.class,
                            () -> client.scan("prefix:", "0").join());
            assertInstanceOf(UnsupportedOperationException.class, failure.getCause());
        }
    }
}
