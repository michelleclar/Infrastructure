package org.carl.infrastructure.pulsar.factory;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.pulsar.config.GlobalShare;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

/** 消费者构建器 提供流式 API 来配置和创建消费者 */
class PulsarConsumerBuilder<T> {

    private final MsgArgsConfig.ConsumerConfig consumerConfig;
    private static final Logger logger = Logger.getLogger(ProcessorBuilder.class);
    private final String topic;
    private final Schema<T> schema;
    private final PulsarClient pulsarClient;

    public static <T> PulsarConsumerBuilder<T> create(
            PulsarClient client, String topic, Class<T> clazz) {
        return new PulsarConsumerBuilder<>(client, topic, Schema.AVRO(clazz));
    }

    public static <T> PulsarConsumerBuilder<T> create(
            PulsarClient client,
            String topic,
            Class<T> clazz,
            MsgArgsConfig.ConsumerConfig consumerConfig) {
        return new PulsarConsumerBuilder<>(client, topic, Schema.AVRO(clazz), consumerConfig);
    }

    public static PulsarConsumerBuilder<byte[]> create(PulsarClient client, String topic) {
        return new PulsarConsumerBuilder<>(client, topic, Schema.AUTO_PRODUCE_BYTES());
    }

    public static PulsarConsumerBuilder<byte[]> create(
            PulsarClient client, String topic, MsgArgsConfig.ConsumerConfig consumerConfig) {
        return new PulsarConsumerBuilder<>(
                client, topic, Schema.AUTO_PRODUCE_BYTES(), consumerConfig);
    }

    private PulsarConsumerBuilder(PulsarClient client, String topic, Schema<T> schema) {
        this.pulsarClient = client;
        this.topic = topic;
        this.schema = schema;
        this.consumerConfig = GlobalShare.getInstance().consumerConfig();
    }

    private PulsarConsumerBuilder(
            PulsarClient client,
            String topic,
            Schema<T> schema,
            MsgArgsConfig.ConsumerConfig consumerConfig) {
        this.pulsarClient = client;
        this.topic = topic;
        this.schema = schema;
        this.consumerConfig = consumerConfig;
    }

    public ConsumerBuilder<T> build(String subscriptionName) {
        org.apache.pulsar.client.api.ConsumerBuilder<T> consumerBuilder =
                pulsarClient
                        .newConsumer(schema)
                        .topic(topic)
                        .subscriptionName(subscriptionName)
                        .subscriptionType(consumerConfig.subscriptionType())
                        .ackTimeout((int) consumerConfig.ackTimeout().toSeconds(), TimeUnit.SECONDS)
                        .ackTimeoutTickTime(
                                (int) consumerConfig.ackTimeoutTickTime().toSeconds(),
                                TimeUnit.SECONDS)
                        .negativeAckRedeliveryDelay(
                                (int) consumerConfig.negativeAckRedeliveryDelay().toSeconds(),
                                TimeUnit.SECONDS)
                        .receiverQueueSize(consumerConfig.receiverQueueSize())
                        .priorityLevel(consumerConfig.priority())
                        .readCompacted(consumerConfig.readCompacted());

        // 设置订阅初始位置
        try {
            consumerBuilder.subscriptionInitialPosition(
                    consumerConfig.subscriptionInitialPosition());
        } catch (IllegalArgumentException e) {
            logger.warnf(
                    "Invalid subscription initial position: %s, using LATEST",
                    consumerConfig.subscriptionInitialPosition());
            consumerBuilder.subscriptionInitialPosition(SubscriptionInitialPosition.Latest);
        }

        // 配置死信队列
        if (consumerConfig.maxRedeliverCount() > 0) {
            consumerBuilder.deadLetterPolicy(
                    DeadLetterPolicy.builder()
                            .maxRedeliverCount(consumerConfig.maxRedeliverCount())
                            .deadLetterTopic(topic + consumerConfig.deadLetterTopicSuffix())
                            .retryLetterTopic(topic + consumerConfig.retryTopicSuffix())
                            .build());
        }

        // 配置批量接收
        if (consumerConfig.batchReceiveEnabled()) {
            consumerBuilder.enableBatchIndexAcknowledgment(true);
        }

        return consumerBuilder;
    }

    public MsgArgsConfig.ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }
    //
    //    /** 设置订阅名称 */
    //    public ConsumerBuilder<T> subscription(String subscription) {
    //        this.subscription = subscription;
    //        return this;
    //    }
    //
    //    /** 设置订阅类型为独占模式 */
    //    public ConsumerBuilder<T> exclusive() {
    //        this.subscriptionType = SubscriptionType.Exclusive;
    //        return this;
    //    }
    //
    //    /** 设置订阅类型为共享模式 */
    //    public ConsumerBuilder<T> shared() {
    //        this.subscriptionType = SubscriptionType.Shared;
    //        return this;
    //    }
    //
    //    /** 设置订阅类型为故障转移模式 */
    //    public ConsumerBuilder<T> failover() {
    //        this.subscriptionType = SubscriptionType.Failover;
    //        return this;
    //    }
    //
    //    /** 设置订阅类型为按键共享模式 */
    //    public ConsumerBuilder<T> keyShared() {
    //        this.subscriptionType = SubscriptionType.Key_Shared;
    //        return this;
    //    }
    //
    //    /** 设置消息处理器 */
    //    public ConsumerBuilder<T> handler(Function<T, Void> handler) {
    //        this.messageHandler = handler;
    //        return this;
    //    }
    //
    //    /** 设置消息处理器（简化版，无返回值） */
    //    public ConsumerBuilder<T> handler(java.util.function.Consumer<T> handler) {
    //        this.messageHandler =
    //                message -> {
    //                    handler.accept(message);
    //                    return null;
    //                };
    //        return this;
    //    }
    //
    //    /** 设置确认超时时间（秒） */
    //    public ConsumerBuilder<T> ackTimeout(int seconds) {
    //        this.config.ackTimeout(seconds);
    //        return this;
    //    }
    //
    //    /** 设置最大重试次数 */
    //    public ConsumerBuilder<T> maxRetries(int retries) {
    //        this.config.maxRetries(retries);
    //        return this;
    //    }
    //
    //    /** 设置死信队列主题 */
    //    public ConsumerBuilder<T> deadLetterTopic(String topic) {
    //        this.config.deadLetterTopic(topic);
    //        return this;
    //    }
    //
    //    /** 设置消费者优先级（用于 Failover 模式） */
    //    public ConsumerBuilder<T> priority(int priority) {
    //        this.config.priority(priority);
    //        return this;
    //    }
    //
    //    /** 构建并启动消费者 */
    //    public Consumer<byte[]> subscribe() throws PulsarClientException {
    //        if (subscription == null || subscription.isEmpty()) {
    //            throw new IllegalArgumentException("Subscription name is required");
    //        }
    //
    //        if (messageHandler == null) {
    //            throw new IllegalArgumentException("Message handler is required");
    //        }
    //
    //        return messageManager.createConsumer(
    //                topic, subscription, subscriptionType, messageHandler, messageType, config);
    //    }
}
