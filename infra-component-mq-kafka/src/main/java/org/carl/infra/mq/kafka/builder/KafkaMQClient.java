package org.carl.infra.mq.kafka.builder;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.IConsumerBuilder;
import org.carl.infra.mq.producer.IProducerBuilder;
import org.carl.infra.mq.reader.IReaderBuilder;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaMQClient implements MQClient {
    private static final ILogger log = LoggerFactory.getLogger(KafkaMQClient.class);

    private final MQConfig.ClientConfig clientConfig;
    private final MQConfig.ProducerConfig producerConfig;
    private final MQConfig.ConsumerConfig consumerConfig;
    private final Set<AutoCloseable> activeResources = ConcurrentHashMap.newKeySet();
    private volatile boolean closed = false;

    public KafkaMQClient(MQConfig config) {
        this.clientConfig = config.client();
        this.producerConfig = config.producer();
        this.consumerConfig = config.consumer();
    }

    public synchronized void registerResource(AutoCloseable resource) {
        if (closed) {
            try {
                resource.close();
            } catch (Exception e) {
                log.error("Failed to close resource registered after client shutdown", e);
            }
            throw new IllegalStateException("Kafka client is closed");
        }
        activeResources.add(resource);
    }

    public void deregisterResource(AutoCloseable resource) {
        activeResources.remove(resource);
    }

    public MQConfig.ClientConfig getClientConfig() {
        return clientConfig;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Kafka client is closed");
        }
    }

    @Override
    public IProducerBuilder<byte[]> newProducer() {
        ensureOpen();
        return KafkaProducerBuilder.create(this, producerConfig);
    }

    @Override
    public <T> IProducerBuilder<T> newProducer(Class<T> clazz) {
        ensureOpen();
        return KafkaProducerBuilder.create(this, clazz, producerConfig);
    }

    @Override
    public IConsumerBuilder<byte[]> newConsumer() {
        ensureOpen();
        return KafkaConsumerBuilder.create(this, consumerConfig);
    }

    @Override
    public <T> IConsumerBuilder<T> newConsumer(Class<T> clazz) {
        ensureOpen();
        return KafkaConsumerBuilder.create(this, clazz, consumerConfig);
    }

    @Override
    public IReaderBuilder<byte[]> newReader() {
        ensureOpen();
        return KafkaReaderBuilder.create(this);
    }

    @Override
    public <T> IReaderBuilder<T> newReader(Class<T> clazz) {
        ensureOpen();
        return KafkaReaderBuilder.create(this, clazz);
    }

    @Override
    public synchronized void close() throws MQClientException {
        if (closed) {
            return;
        }
        closed = true;
        MQClientException lastEx = null;
        for (AutoCloseable resource : Set.copyOf(activeResources)) {
            try {
                resource.close();
            } catch (Exception e) {
                log.error("Failed to close active resource: {}", e.getMessage(), e);
                lastEx = new MQClientException(e);
            }
        }
        activeResources.clear();
        if (lastEx != null) {
            throw lastEx;
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                close();
            } catch (MQClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void shutdown() throws MQClientException {
        close();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
