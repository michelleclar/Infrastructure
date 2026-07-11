package org.carl.infrastructure.mq.kafka.builder;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.consumer.*;
import org.carl.infrastructure.mq.kafka.config.KafkaConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class KafkaConsumerBuilder<T> implements IConsumerBuilder<T> {

    private static final ILogger log = LoggerFactory.getLogger(KafkaConsumerBuilder.class);

    private final KafkaMQClient client;
    private final Class<T> clazz;
    private final KafkaConfig.KafkaConsumerConfig consumerConfig;
    private final Map<String, Object> customConf = new HashMap<>();

    private List<String> topics = new ArrayList<>();
    private Pattern topicsPattern;
    private String subscriptionName;
    private MessageListener<T> listener;
    private boolean autoAck;
    private boolean startPaused = false;
    private String consumerName;

    public static <T> KafkaConsumerBuilder<T> create(
            KafkaMQClient client, Class<T> clazz, MQConfig.ConsumerConfig consumerConfig) {
        return new KafkaConsumerBuilder<>(client, clazz, consumerConfig);
    }

    public static KafkaConsumerBuilder<byte[]> create(
            KafkaMQClient client, MQConfig.ConsumerConfig consumerConfig) {
        return new KafkaConsumerBuilder<>(client, byte[].class, consumerConfig);
    }

    public KafkaConsumerBuilder(KafkaMQClient client, Class<T> clazz, MQConfig.ConsumerConfig consumerConfig) {
        this.client = client;
        this.clazz = clazz;
        this.consumerConfig = copyConsumerConfig(consumerConfig);
        this.autoAck = this.consumerConfig.autoAck();
    }

    private KafkaConfig.KafkaConsumerConfig copyConsumerConfig(MQConfig.ConsumerConfig source) {
        KafkaConfig.KafkaConsumerConfig copy = new KafkaConfig.KafkaConsumerConfig();
        if (source != null) {
            copy.setAutoAck(source.autoAck());
            if (source.ackTimeout() != null) {
                copy.setAckTimeout(source.ackTimeout());
            }
            if (source.ackTimeoutTickTime() != null) {
                copy.setAckTimeoutTickTime(source.ackTimeoutTickTime());
            }
            if (source.negativeAckRedeliveryDelay() != null) {
                copy.setNegativeAckRedeliveryDelay(source.negativeAckRedeliveryDelay());
            }
            copy.setReceiverQueueSize(source.receiverQueueSize());
            copy.setMaxRedeliverCount(source.maxRedeliverCount());
            copy.setDeadLetterTopicSuffix(source.deadLetterTopicSuffix());
            copy.setRetryTopicSuffix(source.retryTopicSuffix());
            copy.setBatchReceiveEnabled(source.batchReceiveEnabled());
            copy.setBatchReceiveMaxMessages(source.batchReceiveMaxMessages());
            if (source.batchReceiveTimeout() != null) {
                copy.setBatchReceiveTimeout(source.batchReceiveTimeout());
            }
            if (source.subscriptionInitialPosition() != null) {
                copy.setSubscriptionInitialPosition(source.subscriptionInitialPosition());
            }
            copy.setPriority(source.priority());
            copy.setReadCompacted(source.readCompacted());
            if (source.subscriptionType() != null) {
                copy.setSubscriptionType(source.subscriptionType());
            }
        }
        return copy;
    }

    @Override
    public IConsumerBuilder<T> autoAck(boolean flag) {
        this.autoAck = flag;
        return this;
    }

    @Override
    public IConsumerBuilder<T> clone() {
        KafkaConsumerBuilder<T> copy = new KafkaConsumerBuilder<>(client, clazz, consumerConfig);
        copy.topics = new ArrayList<>(this.topics);
        copy.topicsPattern = this.topicsPattern;
        copy.subscriptionName = this.subscriptionName;
        copy.listener = this.listener;
        copy.autoAck = this.autoAck;
        copy.startPaused = this.startPaused;
        copy.consumerName = this.consumerName;
        copy.customConf.putAll(this.customConf);
        return copy;
    }

    @Override
    @Deprecated
    public IConsumerBuilder<T> loadConf(Map<String, Object> config) {
        if (config != null) {
            customConf.putAll(config);
        }
        return this;
    }

    @Override
    public IConsumerBuilder<T> conf(Consumer<MQConfig.ConsumerConfig> config) {
        config.accept(this.consumerConfig);
        return this;
    }

    @Override
    public IConsumerBuilder<T> overiteConf(MQConfig.ConsumerConfig config) {
        KafkaConfig.KafkaConsumerConfig copy = copyConsumerConfig(config);
        this.consumerConfig.setAutoAck(copy.autoAck());
        this.consumerConfig.setAckTimeout(copy.ackTimeout());
        this.consumerConfig.setAckTimeoutTickTime(copy.ackTimeoutTickTime());
        this.consumerConfig.setNegativeAckRedeliveryDelay(copy.negativeAckRedeliveryDelay());
        this.consumerConfig.setReceiverQueueSize(copy.receiverQueueSize());
        this.consumerConfig.setMaxRedeliverCount(copy.maxRedeliverCount());
        this.consumerConfig.setDeadLetterTopicSuffix(copy.deadLetterTopicSuffix());
        this.consumerConfig.setRetryTopicSuffix(copy.retryTopicSuffix());
        this.consumerConfig.setBatchReceiveEnabled(copy.batchReceiveEnabled());
        this.consumerConfig.setBatchReceiveMaxMessages(copy.batchReceiveMaxMessages());
        this.consumerConfig.setBatchReceiveTimeout(copy.batchReceiveTimeout());
        this.consumerConfig.setSubscriptionInitialPosition(copy.subscriptionInitialPosition());
        this.consumerConfig.setPriority(copy.priority());
        this.consumerConfig.setReadCompacted(copy.readCompacted());
        this.consumerConfig.setSubscriptionType(copy.subscriptionType());
        return this;
    }

    @Override
    @Deprecated
    public IConsumer<T> subscribe() throws ConsumerException {
        if (subscriptionName == null || subscriptionName.isEmpty()) {
            throw new ConsumerException(new IllegalArgumentException("Subscription name (group.id) must be specified"));
        }
        Map<String, Object> props = buildConsumerProperties();
        org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kc = null;
        try {
            kc = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

            if (topicsPattern != null) {
                kc.subscribe(topicsPattern);
            } else if (topics != null && !topics.isEmpty()) {
                kc.subscribe(topics);
            } else {
                throw new ConsumerException(new IllegalArgumentException("No topics or topics pattern specified"));
            }

            KafkaConsumer<T> consumer = new KafkaConsumer<>(client, kc, clazz, consumerConfig, listener, subscriptionName, autoAck, startPaused);
            client.registerResource(consumer);
            return consumer;
        } catch (Exception e) {
            if (kc != null) {
                kc.close();
            }
            throw new ConsumerException(e);
        }
    }

    @Override
    public IConsumer<T> subscribe(String... topic) throws ConsumerException {
        this.topics = Arrays.asList(topic);
        return subscribe();
    }

    @Override
    public CompletableFuture<IConsumer<T>> subscribeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return subscribe();
            } catch (ConsumerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public IConsumerBuilder<T> topic(String... topicNames) {
        this.topics = Arrays.asList(topicNames);
        return this;
    }

    @Override
    public IConsumerBuilder<T> topics(List<String> topicNames) {
        this.topics = new ArrayList<>(topicNames);
        return this;
    }

    @Override
    public IConsumerBuilder<T> topicsPattern(Pattern topicsPattern) {
        this.topicsPattern = topicsPattern;
        return this;
    }

    @Override
    public IConsumerBuilder<T> topicsPattern(String topicsPattern) {
        this.topicsPattern = Pattern.compile(topicsPattern);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionProperties(Map<String, String> subscriptionProperties) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> ackTimeout(long ackTimeout, TimeUnit timeUnit) {
        this.consumerConfig.setAckTimeout(Duration.ofMillis(timeUnit.toMillis(ackTimeout)));
        return this;
    }

    @Override
    public IConsumerBuilder<T> isAckReceiptEnabled(boolean isAckReceiptEnabled) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> ackTimeoutTickTime(long tickTime, TimeUnit timeUnit) {
        this.consumerConfig.setAckTimeoutTickTime(Duration.ofMillis(timeUnit.toMillis(tickTime)));
        return this;
    }

    @Override
    public IConsumerBuilder<T> negativeAckRedeliveryDelay(long redeliveryDelay, TimeUnit timeUnit) {
        this.consumerConfig.setNegativeAckRedeliveryDelay(Duration.ofMillis(timeUnit.toMillis(redeliveryDelay)));
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionType(SubscriptionType subscriptionType) {
        this.consumerConfig.setSubscriptionType(subscriptionType);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionMode(SubscriptionMode subscriptionMode) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> messageListener(MessageListener<T> messageListener) {
        this.listener = messageListener;
        return this;
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(String privateKey) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(Map<String, String> privateKeys) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> receiverQueueSize(int receiverQueueSize) {
        this.consumerConfig.setReceiverQueueSize(receiverQueueSize);
        return this;
    }

    @Override
    public IConsumerBuilder<T> acknowledgmentGroupTime(long delay, TimeUnit unit) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxAcknowledgmentGroupSize(int messageNum) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> replicateSubscriptionState(boolean replicateSubscriptionState) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxTotalReceiverQueueSizeAcrossPartitions(int maxTotalReceiverQueueSizeAcrossPartitions) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> consumerName(String consumerName) {
        this.consumerName = consumerName;
        return this;
    }

    @Override
    public IConsumerBuilder<T> readCompacted(boolean readCompacted) {
        this.consumerConfig.setReadCompacted(readCompacted);
        return this;
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int periodInMinutes) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int interval, TimeUnit unit) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> priorityLevel(int priorityLevel) {
        this.consumerConfig.setPriority(priorityLevel);
        return this;
    }

    @Override
    public IConsumerBuilder<T> property(String key, String value) {
        customConf.put(key, value);
        return this;
    }

    @Override
    public IConsumerBuilder<T> properties(Map<String, String> properties) {
        if (properties != null) {
            customConf.putAll(properties);
        }
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionInitialPosition(SubscriptionInitialPosition subscriptionInitialPosition) {
        this.consumerConfig.setSubscriptionInitialPosition(subscriptionInitialPosition);
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitions(boolean autoUpdate) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> startMessageIdInclusive() {
        return this;
    }

    @Override
    public IConsumerBuilder<T> enableRetry(boolean retryEnable) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> enableBatchIndexAcknowledgment(boolean batchIndexAcknowledgmentEnabled) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> maxPendingChunkedMessage(int maxPendingChunkedMessage) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoAckOldestChunkedMessageOnQueueFull(boolean autoAckOldestChunkedMessageOnQueueFull) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> expireTimeOfIncompleteChunkedMessage(long duration, TimeUnit unit) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> poolMessages(boolean poolMessages) {
        return this;
    }

    @Override
    public IConsumerBuilder<T> startPaused(boolean paused) {
        this.startPaused = paused;
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoScaledReceiverQueueSizeEnabled(boolean enabled) {
        return this;
    }

    private Map<String, Object> buildConsumerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", client.getClientConfig().serviceUrl());
        props.put("group.id", subscriptionName);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("enable.auto.commit", "false");

        if (consumerConfig.subscriptionInitialPosition() == SubscriptionInitialPosition.Earliest) {
            props.put("auto.offset.reset", "earliest");
        } else {
            props.put("auto.offset.reset", "latest");
        }

        if (consumerName != null) {
            props.put("client.id", consumerName);
        }

        props.putAll(customConf);
        return props;
    }
}
