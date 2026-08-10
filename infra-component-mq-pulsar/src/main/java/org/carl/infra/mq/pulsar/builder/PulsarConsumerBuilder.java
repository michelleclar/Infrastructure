package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.common.ex.ConsumerException;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.DeadLetterPolicy;
import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.consumer.IConsumerBuilder;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.consumer.SubscriptionMode;
import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.consumer.SubscriptionModes;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.pulsar.consumer.PulsarSubscriptionModes;
import org.carl.infra.mq.pulsar.consumer.PulsarSubscriptionTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 消费者构建器 提供流式 API 来配置和创建消费者
 *
 * <p>pulsar consumer builder 装饰器
 */
class PulsarConsumerBuilder<T> implements IConsumerBuilder<T> {

    private static final ILogger log = LoggerFactory.getLogger(PulsarConsumerBuilder.class);
    private final Schema<T> schema;
    private final PulsarClient pulsarClient;
    private org.carl.infra.mq.consumer.MessageListener<T> messageListener;
    private boolean autoAck = false;
    private List<String> topics;
    private final MQConfig.ConsumerConfig consumerConfig;
    private final ConsumerBuilder<T> consumerBuilder;
    private final PulsarTopicResolver topicResolver;

    public static <T> PulsarConsumerBuilder<T> create(
            PulsarClient client, Class<T> clazz, MQConfig.ConsumerConfig consumerConfig) {
        return create(client, clazz, consumerConfig, PulsarTopicResolver.defaults());
    }

    static <T> PulsarConsumerBuilder<T> create(
            PulsarClient client,
            Class<T> clazz,
            MQConfig.ConsumerConfig consumerConfig,
            PulsarTopicResolver topicResolver) {
        return new PulsarConsumerBuilder<>(client, Schema.AVRO(clazz), consumerConfig, topicResolver);
    }

    public static PulsarConsumerBuilder<byte[]> create(
            PulsarClient client, MQConfig.ConsumerConfig consumerConfig) {
        return create(client, consumerConfig, PulsarTopicResolver.defaults());
    }

    static PulsarConsumerBuilder<byte[]> create(
            PulsarClient client,
            MQConfig.ConsumerConfig consumerConfig,
            PulsarTopicResolver topicResolver) {
        // Schema.BYTES (not AUTO_PRODUCE_BYTES, which is a producer-only schema and throws
        // "Schema is not initialized before used" when used to consume a schema'd topic).
        // BYTES delivers the raw message payload regardless of the topic's registered schema.
        return new PulsarConsumerBuilder<>(client, Schema.BYTES, consumerConfig, topicResolver);
    }

    private PulsarConsumerBuilder(
            PulsarClient client, Schema<T> schema, MQConfig.ConsumerConfig consumerConfig) {
        this(client, schema, consumerConfig, PulsarTopicResolver.defaults());
    }

    private PulsarConsumerBuilder(
            PulsarClient client,
            Schema<T> schema,
            MQConfig.ConsumerConfig consumerConfig,
            PulsarTopicResolver topicResolver) {
        this(client, schema, consumerConfig, client.newConsumer(schema), topicResolver);
        applyConfig();
    }

    private PulsarConsumerBuilder(
            PulsarClient client,
            Schema<T> schema,
            MQConfig.ConsumerConfig consumerConfig,
            ConsumerBuilder<T> consumerBuilder,
            PulsarTopicResolver topicResolver) {
        this.pulsarClient = client;
        this.schema = schema;
        this.consumerConfig = consumerConfig;
        this.consumerBuilder = consumerBuilder;
        this.topicResolver = topicResolver;
    }

    @Override
    public IConsumerBuilder<T> autoAck(boolean flag) {
        this.autoAck = flag;
        return this;
    }

    @Override
    public IConsumerBuilder<T> clone() {
        PulsarConsumerBuilder<T> copy =
                new PulsarConsumerBuilder<>(
                        this.pulsarClient,
                        this.schema,
                        this.consumerConfig,
                        this.consumerBuilder.clone(),
                        this.topicResolver);
        copy.messageListener = this.messageListener;
        copy.autoAck = this.autoAck;
        copy.topics = this.topics == null ? null : new ArrayList<>(this.topics);
        return copy;
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
                                    if (autoAck) consumer.acknowledge(msg);
                                } catch (Exception e) {
                                    if (autoAck) consumer.negativeAcknowledge(msg);
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        log.error(
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
                                        log.error(
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
            log.error("subscribe failed ", e);
            throw new ConsumerException(e);
        }
    }

    @Override
    public IConsumer<T> subscribe(String... topics) throws ConsumerException {
        this.recordTopic(topics);
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
                                    if (autoAck) consumer.acknowledge(msg);
                                } catch (Exception e) {
                                    if (autoAck) consumer.negativeAcknowledge(msg);
                                    try {
                                        PulsarConsumer<T> pulsarConsumer = consumerFuture.get();
                                        messageListener.onException(pulsarConsumer, e);
                                    } catch (Exception ex) {
                                        log.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                        throw new RuntimeException(new ConsumerException(ex));
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
                                    } catch (InterruptedException
                                            | ExecutionException
                                            | ConsumerException ex) {
                                        log.error(
                                                "Failed to get consumer reference for exception handling",
                                                ex);
                                        throw new RuntimeException(new ConsumerException(ex));
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
            log.error("subscribe failed ", e);
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<IConsumer<T>> subscribeAsync() {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return subscribe();
                    } catch (ConsumerException error) {
                        throw new java.util.concurrent.CompletionException(error);
                    }
                });
    }

    private void applyConfig() {
        if (consumerConfig == null) {
            return;
        }
        autoAck = consumerConfig.autoAck();
        if (consumerConfig.ackTimeout() != null) {
            consumerBuilder.ackTimeout(consumerConfig.ackTimeout().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (consumerConfig.ackTimeoutTickTime() != null) {
            consumerBuilder.ackTimeoutTickTime(
                    consumerConfig.ackTimeoutTickTime().toMillis(), TimeUnit.MILLISECONDS);
        }
        if (consumerConfig.negativeAckRedeliveryDelay() != null) {
            consumerBuilder.negativeAckRedeliveryDelay(
                    consumerConfig.negativeAckRedeliveryDelay().toMillis(), TimeUnit.MILLISECONDS);
        }
        consumerBuilder.receiverQueueSize(consumerConfig.receiverQueueSize());
        consumerBuilder.priorityLevel(consumerConfig.priority());
        consumerBuilder.readCompacted(consumerConfig.readCompacted());
        if (consumerConfig.subscriptionInitialPosition() != null) {
            subscriptionInitialPosition(consumerConfig.subscriptionInitialPosition());
        }
        if (consumerConfig.subscriptionType() != null) {
            subscriptionType(consumerConfig.subscriptionType());
        }
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
        consumerBuilder.topicsPattern(topicResolver.resolve(topicsPattern));
        return this;
    }

    @Override
    public IConsumerBuilder<T> topicsPattern(String topicsPattern) {
        consumerBuilder.topicsPattern(topicResolver.resolvePatternText(topicsPattern));
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
        final org.apache.pulsar.client.api.SubscriptionType type;
        if (subscriptionType == SubscriptionTypes.LOAD_BALANCED
                || subscriptionType == PulsarSubscriptionTypes.SHARED) {
            type = org.apache.pulsar.client.api.SubscriptionType.Shared;
        } else if (subscriptionType == PulsarSubscriptionTypes.FAILOVER) {
            type = org.apache.pulsar.client.api.SubscriptionType.Failover;
        } else if (subscriptionType == PulsarSubscriptionTypes.KEY_SHARED) {
            type = org.apache.pulsar.client.api.SubscriptionType.Key_Shared;
        } else if (subscriptionType == PulsarSubscriptionTypes.EXCLUSIVE) {
            type = org.apache.pulsar.client.api.SubscriptionType.Exclusive;
        } else {
            throw new UnsupportedMQCapabilityException(
                    "pulsar", "subscription type", subscriptionType);
        }
        consumerBuilder.subscriptionType(type);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionMode(SubscriptionMode subscriptionMode) {
        final org.apache.pulsar.client.api.SubscriptionMode mode;
        if (subscriptionMode == SubscriptionModes.DURABLE) {
            mode = org.apache.pulsar.client.api.SubscriptionMode.Durable;
        } else if (subscriptionMode == PulsarSubscriptionModes.NON_DURABLE) {
            mode = org.apache.pulsar.client.api.SubscriptionMode.NonDurable;
        } else {
            throw new UnsupportedMQCapabilityException(
                    "pulsar", "subscription mode", subscriptionMode);
        }
        consumerBuilder.subscriptionMode(mode);
        return this;
    }

    @Override
    public IConsumerBuilder<T> messageListener(
            org.carl.infra.mq.consumer.MessageListener<T> messageListener) {
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
    public IConsumerBuilder<T> deadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
        Objects.requireNonNull(deadLetterPolicy, "deadLetterPolicy must not be null");
        org.apache.pulsar.client.api.DeadLetterPolicy pulsarPolicy =
                org.apache.pulsar.client.api.DeadLetterPolicy.builder()
                        .maxRedeliverCount(deadLetterPolicy.maxRedeliverCount())
                        .retryLetterTopic(resolveOptionalTopic(deadLetterPolicy.retryLetterTopic()))
                        .deadLetterTopic(resolveOptionalTopic(deadLetterPolicy.deadLetterTopic()))
                        .initialSubscriptionName(deadLetterPolicy.initialSubscriptionName())
                        .build();
        this.consumerBuilder.deadLetterPolicy(pulsarPolicy);
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
        String[] resolvedTopics = topicResolver.resolve(topicNames);
        this.consumerBuilder.topic(resolvedTopics);
        this.topics = Arrays.asList(resolvedTopics);
        return this;
    }

    private IConsumerBuilder<T> recordTopic(List<String> topicNames) {
        List<String> resolvedTopics = topicResolver.resolve(topicNames);
        this.consumerBuilder.topics(resolvedTopics);
        this.topics = resolvedTopics;
        return this;
    }

    private String resolveOptionalTopic(String topic) {
        return topic == null ? null : topicResolver.resolve(topic);
    }

    private void logErrorTopic() {
        if (this.topics == null || this.topics.isEmpty()) {
            log.error("Consumer topic is null");
            return;
        }
        log.error("Consumer topic {}", this.topics);
    }
}
