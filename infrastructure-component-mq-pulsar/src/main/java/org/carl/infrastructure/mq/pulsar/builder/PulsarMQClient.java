package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.consumer.IConsumerBuilder;
import org.carl.infrastructure.mq.producer.IProducerBuilder;

import java.util.concurrent.CompletableFuture;

public class PulsarMQClient implements MQClient {
    private final ILogger log = LoggerFactory.getLogger(PulsarMQClient.class);
    private final PulsarClient pulsarClient;

    public PulsarMQClient(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
    }

    @Override
    public IProducerBuilder<byte[]> newProducer() {
        return PulsarProducerBuilder.create(pulsarClient);
    }

    @Override
    public <T> IProducerBuilder<T> newProducer(Class<T> clazz) {
        return PulsarProducerBuilder.create(pulsarClient, clazz);
    }

    @Override
    public IConsumerBuilder<byte[]> newConsumer() {
        return PulsarConsumerBuilder.create(pulsarClient);
    }

    @Override
    public <T> IConsumerBuilder<T> newConsumer(Class<T> clazz) {
        return PulsarConsumerBuilder.create(pulsarClient, clazz);
    }

    @Override
    public void close() throws MQClientException {
        try {
            pulsarClient.close();
        } catch (PulsarClientException e) {
            log.error(e.getMessage(), e);
            throw new MQClientException(e);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return pulsarClient.closeAsync();
    }

    @Override
    public void shutdown() throws MQClientException {
        try {
            pulsarClient.shutdown();
        } catch (PulsarClientException e) {
            log.error(e.getMessage(), e);
            throw new MQClientException(e);
        }
    }

    @Override
    public boolean isClosed() {
        return pulsarClient.isClosed();
    }
}
