package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.consumer.IConsumerBuilder;
import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionMode;
import org.carl.infrastructure.mq.consumer.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 消费者构建器 提供流式 API 来配置和创建消费者
 *
 * <p>pulsar consumer builder 装饰器
 */
public class PulsarConsumerBuilder<T> implements IConsumerBuilder<T> {

    private static final Logger logger = LoggerFactory.getLogger(PulsarConsumerBuilder.class);
    private final Schema<T> schema;
    private final PulsarClient pulsarClient;
    private org.carl.infrastructure.mq.consumer.MessageListener<T> messageListener;
    private List<String> topics;

    private final ConsumerBuilder<T> consumerBuilder;

    public static <T> PulsarConsumerBuilder<T> create(PulsarClient client, Class<T> clazz) {
        return new PulsarConsumerBuilder<>(client, Schema.AVRO(clazz));
    }

    public static PulsarConsumerBuilder<byte[]> create(PulsarClient client) {
        return new PulsarConsumerBuilder<>(client, Schema.AUTO_PRODUCE_BYTES());
    }

    private PulsarConsumerBuilder(PulsarClient client, Schema<T> schema) {
        this.pulsarClient = client;
        this.schema = schema;
        this.consumerBuilder = client.newConsumer(schema);
    }

    private PulsarConsumerBuilder(
            PulsarClient client, Schema<T> schema, ConsumerBuilder<T> consumerBuilder) {
        this.pulsarClient = client;
        this.schema = schema;
        this.consumerBuilder = consumerBuilder;
    }

    @Override
    public IConsumerBuilder<T> clone() {
        if (this.consumerBuilder != null) {
            return new PulsarConsumerBuilder<>(
                    this.pulsarClient, this.schema, this.consumerBuilder.clone());
        }
        return new PulsarConsumerBuilder<>(pulsarClient, schema);
    }

    @Override
    public IConsumerBuilder<T> loadConf(Map<String, Object> config) {
        consumerBuilder.loadConf(config);
        return this;
    }

    @Override
    public IConsumer<T> subscribe() throws ConsumerException {
        try {
            if (messageListener != null) {
                final CompletableFuture<PulsarConsumer<T>> consumerFuture =
                        new CompletableFuture<>();

                MessageListener<T> pulsarMessageListener =
                        new MessageListener<>() {
                            @Override
                            public void received(Consumer<T> consumer, Message<T> msg) {
                                try {
                                    PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                    messageListener.received(
                                            pulsarConsumer,
                                            PulsarMessageBuilder.PulsarMessage.wrapper(msg));
                                    //                            consumer.acknowledge(msg);
                                } catch (Exception e) {
                                    //                            consumer.negativeAcknowledge(msg);
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        logger.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                    }
                                }
                            }

                            @Override
                            public void reachedEndOfTopic(Consumer<T> consumer) {
                                MessageListener.super.reachedEndOfTopic(consumer);
                                try {
                                    PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                    messageListener.reachedEndOfTopic(pulsarConsumer);
                                } catch (Exception e) {
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        logger.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                    }
                                }
                            }
                        };

                this.consumerBuilder.messageListener(pulsarMessageListener);

                Consumer<T> subscribe = consumerBuilder.subscribe();
                PulsarConsumer<T> tPulsarConsumer = new PulsarConsumer<>(subscribe);

                consumerFuture.complete(tPulsarConsumer);

                return tPulsarConsumer;
            } else {
                Consumer<T> subscribe = consumerBuilder.subscribe();
                return new PulsarConsumer<>(subscribe);
            }
        } catch (PulsarClientException e) {
            logErrorTopic();
            logger.error("subscribe failed ", e);
            throw new ConsumerException(e);
        }
    }

    @Override
    public IConsumer<T> subscribe(String... topics) throws ConsumerException {
        this.recordTopic(topics);
        try {
            // 如果设置了 messageListener，需要在 subscribe 之前配置
            if (messageListener != null) {
                // 方案1：使用 CompletableFuture 来延迟初始化
                final CompletableFuture<PulsarConsumer<T>> consumerFuture =
                        new CompletableFuture<>();

                MessageListener<T> messageListener1 =
                        new MessageListener<>() {
                            @Override
                            public void received(Consumer<T> consumer, Message<T> msg) {
                                try {
                                    // 等待 PulsarConsumer 初始化完成
                                    PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                    messageListener.received(
                                            pulsarConsumer,
                                            PulsarMessageBuilder.PulsarMessage.wrapper(msg));
                                    //                            consumer.acknowledge(msg);
                                } catch (Exception e) {
                                    //                            consumer.negativeAcknowledge(msg);
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        // 如果获取 consumer 失败，记录日志
                                        logErrorTopic();
                                        logger.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                    }
                                }
                            }

                            @Override
                            public void reachedEndOfTopic(Consumer<T> consumer) {
                                MessageListener.super.reachedEndOfTopic(consumer);
                                try {
                                    PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                    messageListener.reachedEndOfTopic(pulsarConsumer);
                                } catch (Exception e) {
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        logErrorTopic();
                                        logger.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                    }
                                }
                            }
                        };

                // 在 subscribe 之前设置监听器
                this.consumerBuilder.messageListener(messageListener1);

                // 创建 Consumer
                Consumer<T> subscribe = consumerBuilder.subscribe();
                PulsarConsumer<T> tPulsarConsumer = new PulsarConsumer<>(subscribe);

                // 完成 Future，使监听器能够获取到 PulsarConsumer 实例
                consumerFuture.complete(tPulsarConsumer);

                return tPulsarConsumer;
            } else {
                // 没有设置监听器的情况，直接创建
                Consumer<T> subscribe = consumerBuilder.subscribe();
                return new PulsarConsumer<>(subscribe);
            }
        } catch (PulsarClientException e) {
            logErrorTopic();
            logger.error("Consumer subscribe failed ", e);
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<IConsumer<T>> subscribeAsync() {
        return consumerBuilder.subscribeAsync().thenApply(PulsarConsumer::new);
    }

    @Override
    public IConsumerBuilder<T> topic(String... topicNames) {
        return this.recordTopic(topicNames);
    }

    @Override
    public IConsumerBuilder<T> topics(List<String> topicNames) {
        return this.recordTopic(topicNames);
    }

    @Override
    public IConsumerBuilder<T> topicsPattern(Pattern topicsPattern) {
        consumerBuilder.topicsPattern(topicsPattern);
        return this;
    }

    @Override
    public IConsumerBuilder<T> topicsPattern(String topicsPattern) {
        consumerBuilder.topicsPattern(topicsPattern);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionName(String subscriptionName) {
        consumerBuilder.subscriptionName(subscriptionName);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionProperties(Map<String, String> subscriptionProperties) {
        consumerBuilder.subscriptionProperties(subscriptionProperties);
        return this;
    }

    @Override
    public IConsumerBuilder<T> ackTimeout(long ackTimeout, TimeUnit timeUnit) {
        consumerBuilder.ackTimeout(ackTimeout, timeUnit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> isAckReceiptEnabled(boolean isAckReceiptEnabled) {
        consumerBuilder.isAckReceiptEnabled(isAckReceiptEnabled);
        return this;
    }

    @Override
    public IConsumerBuilder<T> ackTimeoutTickTime(long tickTime, TimeUnit timeUnit) {
        consumerBuilder.ackTimeoutTickTime(tickTime, timeUnit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> negativeAckRedeliveryDelay(long redeliveryDelay, TimeUnit timeUnit) {
        consumerBuilder.negativeAckRedeliveryDelay(redeliveryDelay, timeUnit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionType(SubscriptionType subscriptionType) {
        var type =
                switch (subscriptionType) {
                    case SHARED -> org.apache.pulsar.client.api.SubscriptionType.Shared;
                    case FAILOVER -> org.apache.pulsar.client.api.SubscriptionType.Failover;
                    case KEY_SHARED -> org.apache.pulsar.client.api.SubscriptionType.Key_Shared;
                    case EXCLUSIVE -> org.apache.pulsar.client.api.SubscriptionType.Exclusive;
                };
        consumerBuilder.subscriptionType(type);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionMode(SubscriptionMode subscriptionMode) {
        var mode =
                switch (subscriptionMode) {
                    case Durable -> org.apache.pulsar.client.api.SubscriptionMode.Durable;
                    case NonDurable -> org.apache.pulsar.client.api.SubscriptionMode.NonDurable;
                };
        consumerBuilder.subscriptionMode(mode);
        return this;
    }

    @Override
    public IConsumerBuilder<T> messageListener(
            org.carl.infrastructure.mq.consumer.MessageListener<T> messageListener) {
        this.messageListener = messageListener;
        return this;
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(String privateKey) {
        this.consumerBuilder.defaultCryptoKeyReader(privateKey);
        return this;
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(Map<String, String> privateKeys) {
        this.consumerBuilder.defaultCryptoKeyReader(privateKeys);
        return this;
    }

    @Override
    public IConsumerBuilder<T> receiverQueueSize(int receiverQueueSize) {
        this.consumerBuilder.receiverQueueSize(receiverQueueSize);
        return this;
    }

    @Override
    public IConsumerBuilder<T> acknowledgmentGroupTime(long delay, TimeUnit unit) {
        this.consumerBuilder.acknowledgmentGroupTime(delay, unit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxAcknowledgmentGroupSize(int messageNum) {
        this.consumerBuilder.maxAcknowledgmentGroupSize(messageNum);
        return this;
    }

    @Override
    public IConsumerBuilder<T> replicateSubscriptionState(boolean replicateSubscriptionState) {
        this.consumerBuilder.replicateSubscriptionState(replicateSubscriptionState);
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxTotalReceiverQueueSizeAcrossPartitions(
            int maxTotalReceiverQueueSizeAcrossPartitions) {
        this.consumerBuilder.maxTotalReceiverQueueSizeAcrossPartitions(
                maxTotalReceiverQueueSizeAcrossPartitions);
        return this;
    }

    @Override
    public IConsumerBuilder<T> consumerName(String consumerName) {
        this.consumerBuilder.consumerName(consumerName);
        return this;
    }

    @Override
    public IConsumerBuilder<T> readCompacted(boolean readCompacted) {
        this.consumerBuilder.readCompacted(readCompacted);
        return this;
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int periodInMinutes) {
        this.consumerBuilder.patternAutoDiscoveryPeriod(periodInMinutes);
        return this;
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int interval, TimeUnit unit) {
        this.consumerBuilder.patternAutoDiscoveryPeriod(interval, unit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> priorityLevel(int priorityLevel) {
        this.consumerBuilder.priorityLevel(priorityLevel);
        return this;
    }

    @Override
    public IConsumerBuilder<T> property(String key, String value) {
        this.consumerBuilder.property(key, value);
        return this;
    }

    @Override
    public IConsumerBuilder<T> properties(Map<String, String> properties) {
        this.consumerBuilder.properties(properties);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionInitialPosition(
            SubscriptionInitialPosition subscriptionInitialPosition) {
        var type =
                switch (subscriptionInitialPosition) {
                    case Latest -> org.apache.pulsar.client.api.SubscriptionInitialPosition.Latest;
                    case Earliest ->
                            org.apache.pulsar.client.api.SubscriptionInitialPosition.Earliest;
                };
        this.consumerBuilder.subscriptionInitialPosition(type);
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitions(boolean autoUpdate) {
        this.consumerBuilder.autoUpdatePartitions(autoUpdate);
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit) {
        this.consumerBuilder.autoUpdatePartitionsInterval(interval, unit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> startMessageIdInclusive() {
        this.consumerBuilder.startMessageIdInclusive();
        return this;
    }

    @Override
    public IConsumerBuilder<T> enableRetry(boolean retryEnable) {
        this.consumerBuilder.enableRetry(retryEnable);
        return this;
    }

    @Override
    public IConsumerBuilder<T> enableBatchIndexAcknowledgment(
            boolean batchIndexAcknowledgmentEnabled) {
        this.consumerBuilder.enableBatchIndexAcknowledgment(batchIndexAcknowledgmentEnabled);
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxPendingChunkedMessage(int maxPendingChunkedMessage) {
        this.consumerBuilder.maxPendingChunkedMessage(maxPendingChunkedMessage);
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoAckOldestChunkedMessageOnQueueFull(
            boolean autoAckOldestChunkedMessageOnQueueFull) {
        this.consumerBuilder.autoAckOldestChunkedMessageOnQueueFull(
                autoAckOldestChunkedMessageOnQueueFull);
        return this;
    }

    @Override
    public IConsumerBuilder<T> expireTimeOfIncompleteChunkedMessage(long duration, TimeUnit unit) {
        this.consumerBuilder.expireTimeOfIncompleteChunkedMessage(duration, unit);
        return this;
    }

    @Override
    public IConsumerBuilder<T> poolMessages(boolean poolMessages) {
        this.consumerBuilder.poolMessages(poolMessages);
        return this;
    }

    @Override
    public IConsumerBuilder<T> startPaused(boolean paused) {
        this.consumerBuilder.startPaused(paused);
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoScaledReceiverQueueSizeEnabled(boolean enabled) {
        this.consumerBuilder.autoScaledReceiverQueueSizeEnabled(enabled);
        return this;
    }

    private IConsumerBuilder<T> recordTopic(String... topicNames) {
        this.consumerBuilder.topic(topicNames);
        this.topics = Arrays.asList(topicNames);
        return this;
    }

    private IConsumerBuilder<T> recordTopic(List<String> topicNames) {
        this.consumerBuilder.topics(topicNames);
        this.topics = topicNames;
        return this;
    }

    private void logErrorTopic() {
        if (this.topics.isEmpty()) {
            logger.error("Consumer topic is null");
            return;
        }
        logger.error("Consumer topic {}", this.topics);
    }
}
