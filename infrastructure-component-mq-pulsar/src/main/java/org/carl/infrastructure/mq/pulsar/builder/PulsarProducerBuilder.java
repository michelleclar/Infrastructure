package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.common.ex.ProducerException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.producer.*;
import org.carl.infrastructure.mq.producer.CompressionType;
import org.carl.infrastructure.mq.producer.HashingScheme;
import org.carl.infrastructure.mq.producer.MessageRoutingMode;
import org.carl.infrastructure.mq.producer.ProducerAccessMode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 生产者构建器 提供流式 API 来发送消息 */
class PulsarProducerBuilder<T> implements IProducerBuilder<T> {

    private static final ILogger log = LoggerFactory.getLogger(PulsarProducerBuilder.class);
    private final PulsarClient pulsarClient;
    private final Schema<T> schema;
    private final ProducerBuilder<T> producerBuilder;
    private MQConfig.ProducerConfig producerConfig;

    public static <T> PulsarProducerBuilder<T> create(
            PulsarClient client, Class<T> clazz, MQConfig.ProducerConfig producerConfig) {
        return new PulsarProducerBuilder<>(client, Schema.AVRO(clazz), producerConfig);
    }

    public static PulsarProducerBuilder<byte[]> create(
            PulsarClient client, MQConfig.ProducerConfig producerConfig) {
        return new PulsarProducerBuilder<>(client, Schema.AUTO_PRODUCE_BYTES(), producerConfig);
    }

    public PulsarProducerBuilder(
            PulsarClient client, Schema<T> schema, MQConfig.ProducerConfig producerConfig) {
        this.pulsarClient = client;
        this.schema = schema;
        this.producerBuilder = pulsarClient.newProducer(schema);
        this.producerConfig = producerConfig;
    }

    @Override
    public IProducer<T> create() throws ProducerException {
        try {
            Producer<T> tProducer = producerBuilder.create();
            return new PulsarProducer<>(tProducer, producerConfig);
        } catch (PulsarClientException e) {
            throw new ProducerException(e);
        }
    }

    @Override
    public IProducer<T> create(String topicName) throws ProducerException {
        producerBuilder.topic(topicName);
        try {
            Producer<T> tProducer = producerBuilder.create();
            return new PulsarProducer<>(tProducer, producerConfig);
        } catch (PulsarClientException e) {
            log.error(e.getMessage(), e);
            throw new ProducerException(e);
        }
    }

    @Override
    public CompletableFuture<IProducer<T>> createAsync() {
        return producerBuilder
                .createAsync()
                .thenApply(producer -> new PulsarProducer<>(producer, producerConfig));
    }

    @Override
    public IProducerBuilder<T> loadConf(Map<String, Object> config) {
        producerBuilder.loadConf(config);
        return this;
    }

    @Override
    public IProducerBuilder<T> conf(Consumer<MQConfig.ProducerConfig> config) {
        config.accept(this.producerConfig);
        return this;
    }

    @Override
    public IProducerBuilder<T> overiteConf(MQConfig.ProducerConfig config) {
        this.producerConfig = config;
        return this;
    }

    @Override
    public IProducerBuilder<T> clone() {
        return new PulsarProducerBuilder<>(pulsarClient, schema, producerConfig);
    }

    @Override
    public IProducerBuilder<T> topic(String topicName) {
        producerBuilder.topic(topicName);
        return this;
    }

    @Override
    public IProducerBuilder<T> producerName(String producerName) {
        producerBuilder.producerName(producerName);
        return this;
    }

    @Override
    public IProducerBuilder<T> accessMode(ProducerAccessMode accessMode) {
        var mode =
                switch (accessMode) {
                    case Shared -> org.apache.pulsar.client.api.ProducerAccessMode.Shared;
                    case Exclusive -> org.apache.pulsar.client.api.ProducerAccessMode.Exclusive;
                    case ExclusiveWithFencing ->
                            org.apache.pulsar.client.api.ProducerAccessMode.ExclusiveWithFencing;
                    case WaitForExclusive ->
                            org.apache.pulsar.client.api.ProducerAccessMode.WaitForExclusive;
                };
        this.producerBuilder.accessMode(mode);
        return this;
    }

    @Override
    public IProducerBuilder<T> sendTimeout(int sendTimeout, TimeUnit unit) {
        this.producerBuilder.sendTimeout(sendTimeout, unit);
        return this;
    }

    @Override
    public IProducerBuilder<T> maxPendingMessages(int maxPendingMessages) {
        this.producerBuilder.maxPendingMessages(maxPendingMessages);
        return this;
    }

    @Override
    public IProducerBuilder<T> blockIfQueueFull(boolean blockIfQueueFull) {
        this.producerBuilder.blockIfQueueFull(blockIfQueueFull);
        return this;
    }

    @Override
    public IProducerBuilder<T> messageRoutingMode(MessageRoutingMode messageRoutingMode) {
        var type =
                switch (messageRoutingMode) {
                    case RoundRobinPartition ->
                            org.apache.pulsar.client.api.MessageRoutingMode.RoundRobinPartition;
                    case CustomPartition ->
                            org.apache.pulsar.client.api.MessageRoutingMode.CustomPartition;
                    case SinglePartition ->
                            org.apache.pulsar.client.api.MessageRoutingMode.SinglePartition;
                };
        this.producerBuilder.messageRoutingMode(type);
        return this;
    }

    @Override
    public IProducerBuilder<T> hashingScheme(HashingScheme hashingScheme) {
        var type =
                switch (hashingScheme) {
                    case JavaStringHash ->
                            org.apache.pulsar.client.api.HashingScheme.JavaStringHash;
                    case Murmur3_32Hash ->
                            org.apache.pulsar.client.api.HashingScheme.Murmur3_32Hash;
                };
        this.producerBuilder.hashingScheme(type);
        return this;
    }

    @Override
    public IProducerBuilder<T> compressionType(CompressionType compressionType) {
        var type =
                switch (compressionType) {
                    case LZ4 -> org.apache.pulsar.client.api.CompressionType.LZ4;
                    case ZLIB -> org.apache.pulsar.client.api.CompressionType.ZLIB;
                    case SNAPPY -> org.apache.pulsar.client.api.CompressionType.SNAPPY;
                    case NONE -> org.apache.pulsar.client.api.CompressionType.NONE;
                    case ZSTD -> org.apache.pulsar.client.api.CompressionType.ZSTD;
                };
        this.producerBuilder.compressionType(type);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableBatching(boolean enableBatching) {
        this.producerBuilder.enableBatching(enableBatching);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableChunking(boolean enableChunking) {
        this.producerBuilder.enableChunking(enableChunking);
        return this;
    }

    @Override
    public IProducerBuilder<T> chunkMaxMessageSize(int chunkMaxMessageSize) {
        this.producerBuilder.chunkMaxMessageSize(chunkMaxMessageSize);
        return this;
    }

    @Override
    public IProducerBuilder<T> batchingMaxPublishDelay(long batchDelay, TimeUnit timeUnit) {
        this.producerBuilder.batchingMaxPublishDelay(batchDelay, timeUnit);
        return this;
    }

    @Override
    public IProducerBuilder<T> roundRobinRouterBatchingPartitionSwitchFrequency(int frequency) {
        this.producerBuilder.roundRobinRouterBatchingPartitionSwitchFrequency(frequency);
        return this;
    }

    @Override
    public IProducerBuilder<T> batchingMaxMessages(int batchMessagesMaxMessagesPerBatch) {
        this.producerBuilder.batchingMaxMessages(batchMessagesMaxMessagesPerBatch);
        return this;
    }

    @Override
    public IProducerBuilder<T> batchingMaxBytes(int batchingMaxBytes) {
        this.producerBuilder.batchingMaxBytes(batchingMaxBytes);
        return this;
    }

    @Override
    public IProducerBuilder<T> initialSequenceId(long initialSequenceId) {
        this.producerBuilder.initialSequenceId(initialSequenceId);
        return this;
    }

    @Override
    public IProducerBuilder<T> property(String key, String value) {
        this.producerBuilder.property(key, value);
        return this;
    }

    @Override
    public IProducerBuilder<T> properties(Map<String, String> properties) {
        this.producerBuilder.properties(properties);
        return this;
    }

    @Override
    public IProducerBuilder<T> autoUpdatePartitions(boolean autoUpdate) {
        this.producerBuilder.autoUpdatePartitions(autoUpdate);
        return this;
    }

    @Override
    public IProducerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit) {
        this.producerBuilder.autoUpdatePartitionsInterval(interval, unit);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableMultiSchema(boolean multiSchema) {
        this.producerBuilder.enableMultiSchema(multiSchema);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableLazyStartPartitionedProducers(
            boolean lazyStartPartitionedProducers) {
        this.producerBuilder.enableLazyStartPartitionedProducers(lazyStartPartitionedProducers);
        return this;
    }
}
