package org.carl.infrastructure.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.pulsar.config.GlobalShare;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;

import java.util.concurrent.TimeUnit;

/** 生产者构建器 提供流式 API 来发送消息 */
public class PulsarProducerBuilder<T> {

    private final String topic;
    private final MsgArgsConfig.ProducerConfig producerConfig;
    private final PulsarClient pulsarClient;
    private final Schema<T> schema;

    public static <T> PulsarProducerBuilder<T> create(
            PulsarClient client, String topic, Class<T> clazz) {
        return new PulsarProducerBuilder<>(client, topic, Schema.AVRO(clazz));
    }

    public static <T> PulsarProducerBuilder<T> create(
            PulsarClient client,
            String topic,
            Class<T> clazz,
            MsgArgsConfig.ProducerConfig producerConfig) {
        return new PulsarProducerBuilder<>(client, topic, Schema.AVRO(clazz), producerConfig);
    }

    public static PulsarProducerBuilder<byte[]> create(PulsarClient client, String topic) {
        return new PulsarProducerBuilder<>(client, topic, Schema.AUTO_PRODUCE_BYTES());
    }

    public static PulsarProducerBuilder<byte[]> create(
            PulsarClient client, String topic, MsgArgsConfig.ProducerConfig producerConfig) {
        return new PulsarProducerBuilder<>(
                client, topic, Schema.AUTO_PRODUCE_BYTES(), producerConfig);
    }

    private PulsarProducerBuilder(PulsarClient client, String topic, Schema<T> schema) {
        this.topic = topic;
        this.pulsarClient = client;
        this.producerConfig = GlobalShare.getInstance().producerConfig();
        this.schema = schema;
    }

    private PulsarProducerBuilder(
            PulsarClient client,
            String topic,
            Schema<T> schema,
            MsgArgsConfig.ProducerConfig producerConfig) {
        this.topic = topic;
        this.pulsarClient = client;
        this.producerConfig = producerConfig;
        this.schema = schema;
    }

    /** 创建生产者 */
    public ProducerBuilder<T> createProducer() {
        org.apache.pulsar.client.api.ProducerBuilder<T> producerBuilder =
                pulsarClient
                        .newProducer(schema)
                        .topic(topic)
                        .sendTimeout(
                                (int) producerConfig.sendTimeout().toSeconds(), TimeUnit.SECONDS)
                        .enableBatching(producerConfig.batchingEnabled())
                        .maxPendingMessages(producerConfig.maxPendingMessages())
                        .compressionType(producerConfig.compressionType())
                        .enableChunking(producerConfig.chunkingEnabled())
                        .chunkMaxMessageSize(producerConfig.chunkMaxMessageSize());

        if (producerConfig.batchingEnabled()) {
            producerBuilder
                    .batchingMaxMessages(producerConfig.batchingMaxMessages())
                    .batchingMaxPublishDelay(
                            producerConfig.batchingMaxPublishDelay().toMillis(),
                            TimeUnit.MILLISECONDS)
                    .batchingMaxBytes(producerConfig.batchingMaxBytes());
        }

        // 设置队列满时的处理策略
        producerBuilder.blockIfQueueFull("BLOCK".equals(producerConfig.blockIfQueueFull()));

        return producerBuilder;
    }

    //
    //    /** 发送消息（异步） */
    //    public <T> CompletableFuture<MessageId> send(T message) {
    //        return messageManager.sendMessage(topic, message);
    //    }
    //
    //    /** 发送消息，带路由键（异步） */
    //    public <T> CompletableFuture<MessageId> send(T message, String key) {
    //        return messageManager.sendMessage(topic, message, key);
    //    }
    //
    //    /** 发送消息（同步） */
    //    public <T> MessageId sendSync(T message) throws PulsarClientException {
    //        return messageManager.sendMessageSync(topic, message);
    //    }
    //
    //    /** 发送消息，带路由键（同步） */
    //    public <T> MessageId sendSync(T message, String key) throws PulsarClientException {
    //        return messageManager.sendMessageSync(topic, message, key);
    //    }
}
