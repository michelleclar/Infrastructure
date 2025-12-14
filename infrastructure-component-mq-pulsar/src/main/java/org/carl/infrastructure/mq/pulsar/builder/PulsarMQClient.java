package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.shade.net.jcip.annotations.NotThreadSafe;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.consumer.IConsumerBuilder;
import org.carl.infrastructure.mq.producer.IProducerBuilder;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;

import java.util.concurrent.CompletableFuture;

@NotThreadSafe
class PulsarMQClient implements MQClient {
    private final ILogger log = LoggerFactory.getLogger(PulsarMQClient.class);
    private final PulsarClient pulsarClient;
    private final MQConfig.ProducerConfig producerConfig;
    private final MQConfig.ConsumerConfig consumerConfig;

    public PulsarMQClient(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
        this.producerConfig = new PulsarConfig.PulsarProducerConfig();
        this.consumerConfig = new PulsarConfig.PulsarConsumerConfig();
    }

    public PulsarMQClient(
            PulsarClient pulsarClient,
            MQConfig.ProducerConfig producerConfig,
            MQConfig.ConsumerConfig consumerConfig) {
        this.pulsarClient = pulsarClient;
        this.producerConfig = producerConfig;
        this.consumerConfig = consumerConfig;
    }

    @Override
    public IProducerBuilder<byte[]> newProducer() {
        return PulsarProducerBuilder.create(pulsarClient, producerConfig);
    }

    @Override
    public <T> IProducerBuilder<T> newProducer(Class<T> clazz) {
        return PulsarProducerBuilder.create(pulsarClient, clazz, producerConfig);
    }

    @Override
    public IConsumerBuilder<byte[]> newConsumer() {
        return PulsarConsumerBuilder.create(pulsarClient, consumerConfig);
    }

    @Override
    public <T> IConsumerBuilder<T> newConsumer(Class<T> clazz) {
        return PulsarConsumerBuilder.create(pulsarClient, clazz, consumerConfig);
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
