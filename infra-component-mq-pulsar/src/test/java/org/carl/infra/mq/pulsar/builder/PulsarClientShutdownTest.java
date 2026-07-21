package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.impl.ClientBuilderImpl;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PulsarClientShutdownTest {

    @Test
    void disablesIdleConnectionDetection() throws Exception {
        PulsarConfig config = new PulsarConfig();

        ClientBuilder clientBuilder =
                PulsarClientFactory.getInstance()
                        .processConnect(config.client())
                        .process(config.transaction())
                        .process(config.monitoring())
                        .build();

        ClientBuilderImpl builder = (ClientBuilderImpl) clientBuilder;
        assertEquals(-1, builder.getClientConfigurationData().getConnectionMaxIdleSeconds());
    }

    @Test
    void synchronousCloseReturnsAfterAsyncCloseTimeout() {
        PulsarClient pulsarClient = clientWithCloseFuture(new CompletableFuture<>());
        PulsarConfig config = new PulsarConfig();
        PulsarMQClient client =
                new PulsarMQClient(
                        pulsarClient,
                        config.producer(),
                        config.consumer(),
                        null,
                        Duration.ofMillis(20));

        assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> assertThrows(MQClientException.class, client::close));
    }

    @Test
    void asynchronousCloseCompletesExceptionallyAfterTimeout() {
        PulsarClient pulsarClient = clientWithCloseFuture(new CompletableFuture<>());
        PulsarConfig config = new PulsarConfig();
        PulsarMQClient client =
                new PulsarMQClient(
                        pulsarClient,
                        config.producer(),
                        config.consumer(),
                        null,
                        Duration.ofMillis(20));

        ExecutionException exception =
                assertThrows(
                        ExecutionException.class,
                        () -> client.closeAsync().get(1, TimeUnit.SECONDS));
        assertEquals(TimeoutException.class, exception.getCause().getClass());
    }

    private PulsarClient clientWithCloseFuture(CompletableFuture<Void> closeFuture) {
        return (PulsarClient)
                Proxy.newProxyInstance(
                        PulsarClient.class.getClassLoader(),
                        new Class<?>[] {PulsarClient.class},
                        (proxy, method, args) -> {
                            if ("closeAsync".equals(method.getName())) {
                                return closeFuture;
                            }
                            throw new UnsupportedOperationException(method.getName());
                        });
    }
}
