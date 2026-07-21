package org.carl.infra.mq.kafka.builder;

import org.carl.infra.mq.common.ex.ProducerException;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.kafka.config.KafkaConfig;
import org.carl.infra.mq.producer.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class KafkaProducerBuilder<T> implements IProducerBuilder<T> {

    private final KafkaMQClient client;
    private final Class<T> clazz;
    private final KafkaConfig.KafkaProducerConfig producerConfig;
    private final Map<String, Object> customConf = new HashMap<>();
    private String topic;
    private String producerName;

    public static <T> KafkaProducerBuilder<T> create(
            KafkaMQClient client, Class<T> clazz, MQConfig.ProducerConfig producerConfig) {
        return new KafkaProducerBuilder<>(client, clazz, producerConfig);
    }

    public static KafkaProducerBuilder<byte[]> create(
            KafkaMQClient client, MQConfig.ProducerConfig producerConfig) {
        return new KafkaProducerBuilder<>(client, byte[].class, producerConfig);
    }

    public KafkaProducerBuilder(KafkaMQClient client, Class<T> clazz, MQConfig.ProducerConfig producerConfig) {
        this.client = client;
        this.clazz = clazz;
        this.producerConfig = copyProducerConfig(producerConfig);
    }

    @Override
    public IProducerBuilder<T> option(ProducerOption option) {
        throw unsupported("producer option", option);
    }

    private KafkaConfig.KafkaProducerConfig copyProducerConfig(MQConfig.ProducerConfig source) {
        KafkaConfig.KafkaProducerConfig copy = new KafkaConfig.KafkaProducerConfig();
        if (source != null) {
            copy.setSendTimeout(source.sendTimeout());
            copy.setBatchingEnabled(source.batchingEnabled());
            copy.setBatchingMaxMessages(source.batchingMaxMessages());
            copy.setBatchingMaxPublishDelay(source.batchingMaxPublishDelay());
            copy.setBatchingMaxBytes(source.batchingMaxBytes());
            copy.setMaxPendingMessages(source.maxPendingMessages());
            copy.setBlockIfQueueFull(source.blockIfQueueFull());
            copy.setCompressionType(source.compressionType());
            copy.setChunkingEnabled(source.chunkingEnabled());
            copy.setChunkMaxMessageSize(source.chunkMaxMessageSize());
        }
        return copy;
    }

    @Override
    @Deprecated
    public IProducer<T> create() throws ProducerException {
        if (topic == null || topic.isEmpty()) {
            throw new ProducerException(new IllegalArgumentException("Topic name cannot be empty"));
        }
        Map<String, Object> props = buildProducerProperties();
        try {
            org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> kp =
                    new org.apache.kafka.clients.producer.KafkaProducer<>(props);
            return new KafkaProducer<>(client, topic, kp, producerConfig);
        } catch (Exception e) {
            throw new ProducerException(e);
        }
    }

    @Override
    public IProducer<T> create(String topicName) throws ProducerException {
        this.topic = topicName;
        return create();
    }

    @Override
    public CompletableFuture<IProducer<T>> createAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return create();
            } catch (ProducerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @Deprecated
    public IProducerBuilder<T> loadConf(Map<String, Object> config) {
        if (config != null) {
            customConf.putAll(config);
        }
        return this;
    }

    @Override
    public IProducerBuilder<T> conf(Consumer<MQConfig.ProducerConfig> config) {
        config.accept(this.producerConfig);
        return this;
    }

    @Override
    public IProducerBuilder<T> overiteConf(MQConfig.ProducerConfig config) {
        // overwrite is done by copying
        KafkaConfig.KafkaProducerConfig copy = copyProducerConfig(config);
        this.producerConfig.setSendTimeout(copy.sendTimeout());
        this.producerConfig.setBatchingEnabled(copy.batchingEnabled());
        this.producerConfig.setBatchingMaxMessages(copy.batchingMaxMessages());
        this.producerConfig.setBatchingMaxPublishDelay(copy.batchingMaxPublishDelay());
        this.producerConfig.setBatchingMaxBytes(copy.batchingMaxBytes());
        this.producerConfig.setMaxPendingMessages(copy.maxPendingMessages());
        this.producerConfig.setBlockIfQueueFull(copy.blockIfQueueFull());
        this.producerConfig.setCompressionType(copy.compressionType());
        this.producerConfig.setChunkingEnabled(copy.chunkingEnabled());
        this.producerConfig.setChunkMaxMessageSize(copy.chunkMaxMessageSize());
        return this;
    }

    @Override
    public IProducerBuilder<T> clone() {
        KafkaProducerBuilder<T> clone = new KafkaProducerBuilder<>(client, clazz, producerConfig);
        clone.topic = this.topic;
        clone.producerName = this.producerName;
        clone.customConf.putAll(this.customConf);
        return clone;
    }

    @Override
    @Deprecated
    public IProducerBuilder<T> topic(String topicName) {
        this.topic = topicName;
        return this;
    }

    @Override
    public IProducerBuilder<T> producerName(String producerName) {
        this.producerName = producerName;
        return this;
    }

    @Override
    public IProducerBuilder<T> accessMode(ProducerAccessMode accessMode) {
        if (accessMode != ProducerAccessModes.SHARED) {
            throw unsupported("producer access mode", accessMode);
        }
        return this;
    }

    @Override
    public IProducerBuilder<T> sendTimeout(int sendTimeout, TimeUnit unit) {
        this.producerConfig.setSendTimeout(Duration.ofMillis(unit.toMillis(sendTimeout)));
        return this;
    }

    @Override
    public IProducerBuilder<T> maxPendingMessages(int maxPendingMessages) {
        throw unsupported("maximum pending messages", maxPendingMessages);
    }

    @Override
    public IProducerBuilder<T> blockIfQueueFull(boolean blockIfQueueFull) {
        throw unsupported("block if queue full", blockIfQueueFull);
    }

    @Override
    public IProducerBuilder<T> messageRoutingMode(MessageRoutingMode messageRoutingMode) {
        throw unsupported("message routing mode", messageRoutingMode);
    }

    @Override
    public IProducerBuilder<T> hashingScheme(HashingScheme hashingScheme) {
        throw unsupported("hashing scheme", hashingScheme);
    }

    @Override
    public IProducerBuilder<T> compressionType(CompressionType compressionType) {
        this.producerConfig.setCompressionType(compressionType);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableBatching(boolean enableBatching) {
        this.producerConfig.setBatchingEnabled(enableBatching);
        return this;
    }

    @Override
    public IProducerBuilder<T> enableChunking(boolean enableChunking) {
        throw unsupported("message chunking", enableChunking);
    }

    @Override
    public IProducerBuilder<T> chunkMaxMessageSize(int chunkMaxMessageSize) {
        throw unsupported("chunk maximum message size", chunkMaxMessageSize);
    }

    @Override
    public IProducerBuilder<T> batchingMaxPublishDelay(long batchDelay, TimeUnit timeUnit) {
        this.producerConfig.setBatchingMaxPublishDelay(Duration.ofMillis(timeUnit.toMillis(batchDelay)));
        return this;
    }

    @Override
    public IProducerBuilder<T> roundRobinRouterBatchingPartitionSwitchFrequency(int frequency) {
        throw unsupported("round-robin batching partition switch frequency", frequency);
    }

    @Override
    public IProducerBuilder<T> batchingMaxMessages(int batchMessagesMaxMessagesPerBatch) {
        throw unsupported("batching maximum message count", batchMessagesMaxMessagesPerBatch);
    }

    @Override
    public IProducerBuilder<T> batchingMaxBytes(int batchingMaxBytes) {
        this.producerConfig.setBatchingMaxBytes(batchingMaxBytes);
        return this;
    }

    @Override
    public IProducerBuilder<T> initialSequenceId(long initialSequenceId) {
        throw unsupported("initial sequence id", initialSequenceId);
    }

    @Override
    public IProducerBuilder<T> property(String key, String value) {
        customConf.put(key, value);
        return this;
    }

    @Override
    public IProducerBuilder<T> properties(Map<String, String> properties) {
        if (properties != null) {
            customConf.putAll(properties);
        }
        return this;
    }

    @Override
    public IProducerBuilder<T> autoUpdatePartitions(boolean autoUpdate) {
        throw unsupported("partition auto update switch", autoUpdate);
    }

    @Override
    public IProducerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit) {
        throw unsupported("partition auto update interval", Duration.ofMillis(unit.toMillis(interval)));
    }

    @Override
    public IProducerBuilder<T> enableMultiSchema(boolean multiSchema) {
        throw unsupported("multiple schemas", multiSchema);
    }

    @Override
    public IProducerBuilder<T> enableLazyStartPartitionedProducers(boolean lazyStartPartitionedProducers) {
        throw unsupported("lazy partitioned producers", lazyStartPartitionedProducers);
    }

    private UnsupportedMQCapabilityException unsupported(String capability, Object value) {
        return new UnsupportedMQCapabilityException("kafka", capability, value);
    }

    private Map<String, Object> buildProducerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", client.getClientConfig().serviceUrl());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        // Timeout
        if (producerConfig.sendTimeout() != null) {
            props.put("request.timeout.ms", (int) producerConfig.sendTimeout().toMillis());
        }

        // Batching
        if (producerConfig.batchingEnabled()) {
            props.put("batch.size", producerConfig.batchingMaxBytes());
            props.put("linger.ms", (int) producerConfig.batchingMaxPublishDelay().toMillis());
        } else {
            props.put("batch.size", 0);
            props.put("linger.ms", 0);
        }

        // Compression
        if (producerConfig.compressionType() != null) {
            String kafkaCompression = switch (producerConfig.compressionType()) {
                case LZ4 -> "lz4";
                case ZLIB -> "gzip";
                case SNAPPY -> "snappy";
                case ZSTD -> "zstd";
                case NONE -> "none";
            };
            props.put("compression.type", kafkaCompression);
        }

        // Client/Producer ID
        if (producerName != null) {
            props.put("client.id", producerName);
        }

        // Custom config overrides
        props.putAll(customConf);

        return props;
    }
}
