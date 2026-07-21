package org.carl.infra.mq.client;

import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.IConsumerBuilder;
import org.carl.infra.mq.producer.IProducerBuilder;
import org.carl.infra.mq.reader.IReaderBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MQClientFactoryTest {

    private final MQConfig config = new EmptyConfig();

    @Test
    void shouldCreateClientWithTheOnlyProvider() throws MQClientException {
        TestClient expected = new TestClient();

        MQClient actual =
                MQClientFactory.create(config, List.of(new TestProvider("kafka", expected)));

        assertSame(expected, actual);
    }

    @Test
    void shouldRejectMissingProvider() {
        MQClientException exception =
                assertThrows(
                        MQClientException.class,
                        () -> MQClientFactory.create(config, List.of()));

        assertEquals(
                "No MQ client provider found. Add exactly one MQ provider implementation to the runtime classpath",
                exception.getMessage());
    }

    @Test
    void shouldRejectMultipleProvidersWithStableDiagnosticOrder() {
        TestClient client = new TestClient();

        MQClientException exception =
                assertThrows(
                        MQClientException.class,
                        () ->
                                MQClientFactory.create(
                                        config,
                                        List.of(
                                                new TestProvider("pulsar", client),
                                                new TestProvider("kafka", client))));

        assertEquals(
                "Multiple MQ client providers found: kafka, pulsar. Keep exactly one provider implementation on the runtime classpath",
                exception.getMessage());
    }

    @Test
    void shouldRejectNullClientReturnedByProvider() {
        MQClientException exception =
                assertThrows(
                        MQClientException.class,
                        () ->
                                MQClientFactory.create(
                                        config, List.of(new TestProvider("kafka", null))));

        assertEquals("MQ client provider 'kafka' returned a null client", exception.getMessage());
    }

    private record TestProvider(String name, MQClient client) implements MQClientProvider {
        @Override
        public MQClient create(MQConfig config) {
            return client;
        }
    }

    private static final class EmptyConfig implements MQConfig {
        @Override
        public Optional<String> name() {
            return Optional.empty();
        }

        @Override
        public ClientConfig client() {
            return null;
        }

        @Override
        public ProducerConfig producer() {
            return null;
        }

        @Override
        public ConsumerConfig consumer() {
            return null;
        }

        @Override
        public TransactionConfig transaction() {
            return null;
        }

        @Override
        public MonitoringConfig monitoring() {
            return null;
        }

        @Override
        public RetryConfig retry() {
            return null;
        }
    }

    private static final class TestClient implements MQClient {
        @Override
        public IProducerBuilder<byte[]> newProducer() {
            return null;
        }

        @Override
        public <T> IProducerBuilder<T> newProducer(Class<T> clazz) {
            return null;
        }

        @Override
        public IConsumerBuilder<byte[]> newConsumer() {
            return null;
        }

        @Override
        public <T> IConsumerBuilder<T> newConsumer(Class<T> clazz) {
            return null;
        }

        @Override
        public IReaderBuilder<byte[]> newReader() {
            return null;
        }

        @Override
        public <T> IReaderBuilder<T> newReader(Class<T> clazz) {
            return null;
        }

        @Override
        public void close() {}

        @Override
        public CompletableFuture<Void> closeAsync() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void shutdown() {}

        @Override
        public boolean isClosed() {
            return false;
        }
    }
}
