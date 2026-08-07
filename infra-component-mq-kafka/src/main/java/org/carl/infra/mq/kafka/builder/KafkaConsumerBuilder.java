package org.carl.infra.mq.kafka.builder;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.common.ex.ConsumerException;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.*;
import org.carl.infra.mq.kafka.config.KafkaConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
        validateConfig();
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
        throw unsupported("subscription properties", subscriptionProperties);
    }

    @Override
    public IConsumerBuilder<T> ackTimeout(long ackTimeout, TimeUnit timeUnit) {
        throw unsupported("ack timeout", Duration.ofMillis(timeUnit.toMillis(ackTimeout)));
    }

    @Override
    public IConsumerBuilder<T> isAckReceiptEnabled(boolean isAckReceiptEnabled) {
        throw unsupported("ack receipt", isAckReceiptEnabled);
    }

    @Override
    public IConsumerBuilder<T> ackTimeoutTickTime(long tickTime, TimeUnit timeUnit) {
        throw unsupported("ack timeout tick time", Duration.ofMillis(timeUnit.toMillis(tickTime)));
    }

    @Override
    public IConsumerBuilder<T> negativeAckRedeliveryDelay(long redeliveryDelay, TimeUnit timeUnit) {
        throw unsupported(
                "negative ack redelivery delay",
                Duration.ofMillis(timeUnit.toMillis(redeliveryDelay)));
    }

    @Override
    public IConsumerBuilder<T> subscriptionType(SubscriptionType subscriptionType) {
        if (subscriptionType != SubscriptionTypes.LOAD_BALANCED) {
            throw unsupported("subscription type", subscriptionType);
        }
        this.consumerConfig.setSubscriptionType(SubscriptionTypes.LOAD_BALANCED);
        return this;
    }

    @Override
    public IConsumerBuilder<T> subscriptionMode(SubscriptionMode subscriptionMode) {
        if (subscriptionMode != SubscriptionModes.DURABLE) {
            throw unsupported("subscription mode", subscriptionMode);
        }
        return this;
    }

    @Override
    public IConsumerBuilder<T> messageListener(MessageListener<T> messageListener) {
        this.listener = messageListener;
        return this;
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(String privateKey) {
        throw unsupported("consumer payload decryption", privateKey);
    }

    @Override
    public IConsumerBuilder<T> defaultCryptoKeyReader(Map<String, String> privateKeys) {
        throw unsupported("consumer payload decryption", privateKeys);
    }

    @Override
    public IConsumerBuilder<T> receiverQueueSize(int receiverQueueSize) {
        throw unsupported("receiver queue size", receiverQueueSize);
    }

    @Override
    public IConsumerBuilder<T> acknowledgmentGroupTime(long delay, TimeUnit unit) {
        throw unsupported("acknowledgment group time", Duration.ofMillis(unit.toMillis(delay)));
    }

    @Override
    public IConsumerBuilder<T> maxAcknowledgmentGroupSize(int messageNum) {
        throw unsupported("maximum acknowledgment group size", messageNum);
    }

    @Override
    public IConsumerBuilder<T> replicateSubscriptionState(boolean replicateSubscriptionState) {
        throw unsupported("subscription state replication", replicateSubscriptionState);
    }

    @Override
    public IConsumerBuilder<T> maxTotalReceiverQueueSizeAcrossPartitions(int maxTotalReceiverQueueSizeAcrossPartitions) {
        throw unsupported(
                "maximum receiver queue size across partitions",
                maxTotalReceiverQueueSizeAcrossPartitions);
    }

    @Override
    public IConsumerBuilder<T> consumerName(String consumerName) {
        this.consumerName = consumerName;
        return this;
    }

    @Override
    public IConsumerBuilder<T> readCompacted(boolean readCompacted) {
        throw unsupported("read compacted", readCompacted);
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int periodInMinutes) {
        throw unsupported("pattern auto discovery period", Duration.ofMinutes(periodInMinutes));
    }

    @Override
    public IConsumerBuilder<T> patternAutoDiscoveryPeriod(int interval, TimeUnit unit) {
        throw unsupported("pattern auto discovery period", Duration.ofMillis(unit.toMillis(interval)));
    }

    @Override
    public IConsumerBuilder<T> priorityLevel(int priorityLevel) {
        throw unsupported("consumer priority", priorityLevel);
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
    public IConsumerBuilder<T> deadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
        throw unsupported("dead letter policy", deadLetterPolicy);
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitions(boolean autoUpdate) {
        throw unsupported("partition auto update switch", autoUpdate);
    }

    @Override
    public IConsumerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit) {
        throw unsupported("partition auto update interval", Duration.ofMillis(unit.toMillis(interval)));
    }

    @Override
    public IConsumerBuilder<T> startMessageIdInclusive() {
        throw unsupported("inclusive initial message id", true);
    }

    @Override
    public IConsumerBuilder<T> enableRetry(boolean retryEnable) {
        throw unsupported("broker retry subscription", retryEnable);
    }

    @Override
    public IConsumerBuilder<T> enableBatchIndexAcknowledgment(boolean batchIndexAcknowledgmentEnabled) {
        throw unsupported("batch index acknowledgment", batchIndexAcknowledgmentEnabled);
    }

    @Override
    public IConsumerBuilder<T> maxPendingChunkedMessage(int maxPendingChunkedMessage) {
        throw unsupported("maximum pending chunked messages", maxPendingChunkedMessage);
    }

    @Override
    public IConsumerBuilder<T> autoAckOldestChunkedMessageOnQueueFull(boolean autoAckOldestChunkedMessageOnQueueFull) {
        throw unsupported(
                "auto ack oldest chunked message on queue full",
                autoAckOldestChunkedMessageOnQueueFull);
    }

    @Override
    public IConsumerBuilder<T> expireTimeOfIncompleteChunkedMessage(long duration, TimeUnit unit) {
        throw unsupported(
                "incomplete chunked message expiry", Duration.ofMillis(unit.toMillis(duration)));
    }

    @Override
    public IConsumerBuilder<T> poolMessages(boolean poolMessages) {
        throw unsupported("pooled messages", poolMessages);
    }

    @Override
    public IConsumerBuilder<T> startPaused(boolean paused) {
        this.startPaused = paused;
        return this;
    }

    @Override
    public IConsumerBuilder<T> autoScaledReceiverQueueSizeEnabled(boolean enabled) {
        throw unsupported("auto-scaled receiver queue", enabled);
    }

    private UnsupportedMQCapabilityException unsupported(String capability, Object value) {
        return new UnsupportedMQCapabilityException("kafka", capability, value);
    }

    private void validateConfig() {
        if (consumerConfig.subscriptionType() != SubscriptionTypes.LOAD_BALANCED) {
            throw unsupported("configured subscription type", consumerConfig.subscriptionType());
        }
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
