package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.common.ex.ProducerException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.producer.*;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.pulsar.producer.PulsarHashingSchemes;
import org.carl.infra.mq.pulsar.producer.PulsarMessageRoutingModes;
import org.carl.infra.mq.pulsar.producer.PulsarProducerAccessModes;
import org.carl.infra.mq.pulsar.producer.PulsarProducerOptions;
import org.carl.infra.mq.producer.CompressionType;
import org.carl.infra.mq.producer.HashingScheme;
import org.carl.infra.mq.producer.MessageRoutingMode;
import org.carl.infra.mq.producer.ProducerAccessMode;

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
    private final PulsarTopicResolver topicResolver;
    private MQConfig.ProducerConfig producerConfig;

    public static <T> PulsarProducerBuilder<T> create(
            PulsarClient client, Class<T> clazz, MQConfig.ProducerConfig producerConfig) {
        return create(client, clazz, producerConfig, PulsarTopicResolver.defaults());
    }

    static <T> PulsarProducerBuilder<T> create(
            PulsarClient client,
            Class<T> clazz,
            MQConfig.ProducerConfig producerConfig,
            PulsarTopicResolver topicResolver) {
        return new PulsarProducerBuilder<>(
                client, Schema.AVRO(clazz), producerConfig, topicResolver);
    }

    public static PulsarProducerBuilder<byte[]> create(
            PulsarClient client, MQConfig.ProducerConfig producerConfig) {
        return create(client, producerConfig, PulsarTopicResolver.defaults());
    }

    static PulsarProducerBuilder<byte[]> create(
            PulsarClient client,
            MQConfig.ProducerConfig producerConfig,
            PulsarTopicResolver topicResolver) {
        return new PulsarProducerBuilder<>(
                client, Schema.AUTO_PRODUCE_BYTES(), producerConfig, topicResolver);
    }

    public PulsarProducerBuilder(
            PulsarClient client, Schema<T> schema, MQConfig.ProducerConfig producerConfig) {
        this(client, schema, producerConfig, PulsarTopicResolver.defaults());
    }

    private PulsarProducerBuilder(
            PulsarClient client,
            Schema<T> schema,
            MQConfig.ProducerConfig producerConfig,
            PulsarTopicResolver topicResolver) {
        this(client, schema, producerConfig, client.newProducer(schema), topicResolver);
        applyConfig();
    }

    private PulsarProducerBuilder(
            PulsarClient client,
            Schema<T> schema,
            MQConfig.ProducerConfig producerConfig,
            ProducerBuilder<T> producerBuilder,
            PulsarTopicResolver topicResolver) {
        this.pulsarClient = client;
        this.schema = schema;
        this.producerBuilder = producerBuilder;
        this.producerConfig = producerConfig;
        this.topicResolver = topicResolver;
    }

    @Override
    public IProducerBuilder<T> option(ProducerOption option) {
        PulsarProducerOptions.apply(option, producerBuilder);
        return this;
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
        producerBuilder.topic(topicResolver.resolve(topicName));
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
        applyConfig();
        return this;
    }

    @Override
    public IProducerBuilder<T> overiteConf(MQConfig.ProducerConfig config) {
        this.producerConfig = config;
        applyConfig();
        return this;
    }

    @Override
    public IProducerBuilder<T> clone() {
        return new PulsarProducerBuilder<>(
                pulsarClient,
                schema,
                producerConfig,
                producerBuilder.clone(),
                topicResolver);
    }

    @Override
    public IProducerBuilder<T> topic(String topicName) {
        producerBuilder.topic(topicResolver.resolve(topicName));
        return this;
    }

    @Override
    public IProducerBuilder<T> producerName(String producerName) {
        producerBuilder.producerName(producerName);
        return this;
    }

    @Override
    public IProducerBuilder<T> accessMode(ProducerAccessMode accessMode) {
        final org.apache.pulsar.client.api.ProducerAccessMode mode;
        if (accessMode == ProducerAccessModes.SHARED) {
            mode = org.apache.pulsar.client.api.ProducerAccessMode.Shared;
        } else if (accessMode == PulsarProducerAccessModes.EXCLUSIVE) {
            mode = org.apache.pulsar.client.api.ProducerAccessMode.Exclusive;
        } else if (accessMode == PulsarProducerAccessModes.EXCLUSIVE_WITH_FENCING) {
            mode = org.apache.pulsar.client.api.ProducerAccessMode.ExclusiveWithFencing;
        } else if (accessMode == PulsarProducerAccessModes.WAIT_FOR_EXCLUSIVE) {
            mode = org.apache.pulsar.client.api.ProducerAccessMode.WaitForExclusive;
        } else {
            throw new UnsupportedMQCapabilityException("pulsar", "producer access mode", accessMode);
        }
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
        final org.apache.pulsar.client.api.MessageRoutingMode type;
        if (messageRoutingMode == PulsarMessageRoutingModes.ROUND_ROBIN_PARTITION) {
            type = org.apache.pulsar.client.api.MessageRoutingMode.RoundRobinPartition;
        } else if (messageRoutingMode == PulsarMessageRoutingModes.CUSTOM_PARTITION) {
            type = org.apache.pulsar.client.api.MessageRoutingMode.CustomPartition;
        } else if (messageRoutingMode == PulsarMessageRoutingModes.SINGLE_PARTITION) {
            type = org.apache.pulsar.client.api.MessageRoutingMode.SinglePartition;
        } else {
            throw new UnsupportedMQCapabilityException(
                    "pulsar", "message routing mode", messageRoutingMode);
        }
        this.producerBuilder.messageRoutingMode(type);
        return this;
    }

    @Override
    public IProducerBuilder<T> hashingScheme(HashingScheme hashingScheme) {
        final org.apache.pulsar.client.api.HashingScheme type;
        if (hashingScheme == PulsarHashingSchemes.JAVA_STRING_HASH) {
            type = org.apache.pulsar.client.api.HashingScheme.JavaStringHash;
        } else if (hashingScheme == PulsarHashingSchemes.MURMUR3_32_HASH) {
            type = org.apache.pulsar.client.api.HashingScheme.Murmur3_32Hash;
        } else {
            throw new UnsupportedMQCapabilityException("pulsar", "hashing scheme", hashingScheme);
        }
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

    private void applyConfig() {
        if (producerConfig == null) {
            return;
        }
        if (producerConfig.sendTimeout() != null) {
            producerBuilder.sendTimeout(
                    Math.toIntExact(producerConfig.sendTimeout().toMillis()), TimeUnit.MILLISECONDS);
        }
        producerBuilder.enableBatching(producerConfig.batchingEnabled());
        producerBuilder.batchingMaxMessages(producerConfig.batchingMaxMessages());
        if (producerConfig.batchingMaxPublishDelay() != null) {
            producerBuilder.batchingMaxPublishDelay(
                    producerConfig.batchingMaxPublishDelay().toMillis(), TimeUnit.MILLISECONDS);
        }
        producerBuilder.batchingMaxBytes(producerConfig.batchingMaxBytes());
        producerBuilder.maxPendingMessages(producerConfig.maxPendingMessages());
        producerBuilder.blockIfQueueFull(Boolean.parseBoolean(producerConfig.blockIfQueueFull()));
        if (producerConfig.compressionType() != null) {
            compressionType(producerConfig.compressionType());
        }
        producerBuilder.enableChunking(producerConfig.chunkingEnabled());
        producerBuilder.chunkMaxMessageSize(producerConfig.chunkMaxMessageSize());
    }
}
