package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.shade.net.jcip.annotations.NotThreadSafe;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.IConsumerBuilder;
import org.carl.infra.mq.producer.IProducerBuilder;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.carl.infra.mq.reader.IReaderBuilder;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@NotThreadSafe
class PulsarMQClient implements MQClient {
    private static final Duration DEFAULT_CLOSE_TIMEOUT = Duration.ofSeconds(60);

    private final ILogger log = LoggerFactory.getLogger(PulsarMQClient.class);
    private final PulsarClient pulsarClient;
    private final PulsarAdmin pulsarAdmin;
    private final MQConfig.ProducerConfig producerConfig;
    private final MQConfig.ConsumerConfig consumerConfig;
    private final Duration closeTimeout;

    public PulsarMQClient(PulsarClient pulsarClient) {
        this(pulsarClient, new PulsarConfig.PulsarProducerConfig(), new PulsarConfig.PulsarConsumerConfig(), null);
    }

    public PulsarMQClient(
            PulsarClient pulsarClient,
            MQConfig.ProducerConfig producerConfig,
            MQConfig.ConsumerConfig consumerConfig) {
        this(pulsarClient, producerConfig, consumerConfig, null);
    }

    public PulsarMQClient(
            PulsarClient pulsarClient,
            MQConfig.ProducerConfig producerConfig,
            MQConfig.ConsumerConfig consumerConfig,
            PulsarAdmin pulsarAdmin) {
        this(
                pulsarClient,
                producerConfig,
                consumerConfig,
                pulsarAdmin,
                DEFAULT_CLOSE_TIMEOUT);
    }

    PulsarMQClient(
            PulsarClient pulsarClient,
            MQConfig.ProducerConfig producerConfig,
            MQConfig.ConsumerConfig consumerConfig,
            PulsarAdmin pulsarAdmin,
            Duration closeTimeout) {
        this.pulsarClient = pulsarClient;
        this.producerConfig = producerConfig;
        this.consumerConfig = consumerConfig;
        this.pulsarAdmin = pulsarAdmin;
        this.closeTimeout = closeTimeout;
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
    public IReaderBuilder<byte[]> newReader() {
        return PulsarReaderBuilder.create(pulsarClient, pulsarAdmin);
    }

    @Override
    public <T> IReaderBuilder<T> newReader(Class<T> clazz) {
        return PulsarReaderBuilder.create(pulsarClient, clazz, pulsarAdmin);
    }

    @Override
    public void close() throws MQClientException {
        try {
            closeAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MQClientException("Interrupted while closing Pulsar client", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PulsarClientException.AlreadyClosedException) {
                return;
            }
            log.error("Failed to close Pulsar client", cause);
            throw new MQClientException("Failed to close Pulsar client", cause);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        // 用 whenCompleteAsync 把 PulsarAdmin.close()（阻塞 HTTP）移出 Pulsar 内部 event loop 线程。
        return pulsarClient
                .closeAsync()
                .whenCompleteAsync((v, t) -> closeAdmin())
                .orTimeout(closeTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void closeAdmin() {
        if (pulsarAdmin == null) {
            return;
        }
        try {
            pulsarAdmin.close();
        } catch (Exception e) {
            log.warn("Failed to close PulsarAdmin: {}", e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() throws MQClientException {
        MQClientException clientEx = null;
        try {
            pulsarClient.shutdown();
        } catch (PulsarClientException e) {
            log.error(e.getMessage(), e);
            clientEx = new MQClientException(e);
        }
        closeAdmin();
        if (clientEx != null) {
            throw clientEx;
        }
    }

    @Override
    public boolean isClosed() {
        return pulsarClient.isClosed();
    }
}
