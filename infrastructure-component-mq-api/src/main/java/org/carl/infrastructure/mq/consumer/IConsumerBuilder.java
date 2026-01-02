package org.carl.infrastructure.mq.consumer;

import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.config.MQConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * {@link IConsumerBuilder}
 *
 * @param <T>
 */
public interface IConsumerBuilder<T> extends Cloneable {

    IConsumerBuilder<T> autoAck(boolean flag);

    /**
     * Create a copy of the current consumer builder.
     *
     * <p>Cloning the builder can be used to share an incomplete configuration and specialize it
     * multiple times. For example:
     *
     * <pre>{@code
     * ConsumerBuilder<String> builder = client.newConsumer(Schema.STRING)
     *         .subscriptionName("my-subscription-name")
     *         .subscriptionType(SubscriptionType.Shared)
     *         .receiverQueueSize(10);
     *
     * Consumer<String> consumer1 = builder.clone().topic("my-topic-1").subscribe();
     * Consumer<String> consumer2 = builder.clone().topic("my-topic-2").subscribe();
     * }</pre>
     *
     * @return a cloned consumer builder object
     */
    IConsumerBuilder<T> clone();

    /**
     * Load the configuration from provided <tt>config</tt> map.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Map<String, Object> config = new HashMap<>();
     * config.put("ackTimeoutMillis", 1000);
     * config.put("receiverQueueSize", 2000);
     *
     * Consumer<byte[]> builder = client.newConsumer()
     *              .loadConf(config)
     *              .subscribe();
     *
     * Consumer<byte[]> consumer = builder.subscribe();
     * }</pre>
     *
     * @param config configuration to load
     * @return the consumer builder instance
     */
    @Deprecated
    IConsumerBuilder<T> loadConf(Map<String, Object> config);

    IConsumerBuilder<T> conf(Consumer<MQConfig.ConsumerConfig> config);

    IConsumerBuilder<T> overiteConf(MQConfig.ConsumerConfig config);

    @Deprecated
    IConsumer<T> subscribe() throws ConsumerException;

    IConsumer<T> subscribe(String... topic) throws ConsumerException;

    CompletableFuture<IConsumer<T>> subscribeAsync();

    /**
     * Specify the topics this consumer subscribes to.
     *
     * @param topicNames a set of topics that the consumer subscribes to
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> topic(String... topicNames);

    /**
     * Specify a list of topics that this consumer subscribes to.
     *
     * @param topicNames a list of topics that the consumer subscribes to
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> topics(List<String> topicNames);

    /**
     * Specify a pattern for topics(not contains the partition suffix) that this consumer subscribes
     * to.
     *
     * <p>The pattern is applied to subscribe to all topics, within a single namespace, that match
     * the pattern.
     *
     * <p>The consumer automatically subscribes to topics created after itself.
     *
     * @param topicsPattern a regular expression to select a list of topics(not contains the
     *     partition suffix) to subscribe to
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> topicsPattern(Pattern topicsPattern);

    /**
     * Specify a pattern for topics(not contains the partition suffix) that this consumer subscribes
     * to.
     *
     * <p>It accepts a regular expression that is compiled into a pattern internally. E.g.,
     * "persistent://public/default/pattern-topic-.*"
     *
     * <p>The pattern is applied to subscribe to all topics, within a single namespace, that match
     * the pattern.
     *
     * <p>The consumer automatically subscribes to topics created after itself.
     *
     * @param topicsPattern given regular expression for topics(not contains the partition suffix)
     *     pattern
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> topicsPattern(String topicsPattern);

    /**
     * Specify the subscription name for this consumer.
     *
     * <p>This argument is required when constructing the consumer.
     *
     * @param subscriptionName the name of the subscription that this consumer should attach to
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> subscriptionName(String subscriptionName);

    /**
     * Specify the subscription properties for this subscription. Properties are immutable, and
     * consumers under the same subscription will fail to create a subscription if they use
     * different properties.
     *
     * @param subscriptionProperties the properties of the subscription
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> subscriptionProperties(Map<String, String> subscriptionProperties);

    /**
     * Sets the timeout for unacknowledged messages, truncated to the nearest millisecond. The
     * timeout must be greater than 1 second.
     *
     * <p>By default, the acknowledgment timeout is disabled (set to `0`, which means infinite).
     * When a consumer with an infinite acknowledgment timeout terminates, any unacknowledged
     * messages that it receives are re-delivered to another consumer.
     *
     * <p>When enabling acknowledgment timeout, if a message is not acknowledged within the
     * specified timeout, it is re-delivered to the consumer (possibly to a different consumer, in
     * the case of a shared subscription).
     *
     * @param ackTimeout for unacked messages.
     * @param timeUnit unit in which the timeout is provided.
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> ackTimeout(long ackTimeout, TimeUnit timeUnit);

    /**
     * Acknowledgement returns receipt, but the message is not re-sent after getting receipt.
     *
     * <p>Configure the acknowledgement timeout mechanism to redeliver the message if it is not
     * acknowledged after ackTimeout, or to execute a timer task to check the acknowledgement
     * timeout messages during every ackTimeoutTickTime period.
     *
     * @param isAckReceiptEnabled {@link Boolean} enables acknowledgement for receipt
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> isAckReceiptEnabled(boolean isAckReceiptEnabled);

    /**
     * Define the granularity of the ack-timeout redelivery.
     *
     * <p>By default, the tick time is set to 1 second. Using a higher tick time reduces the memory
     * overhead to track messages when the ack-timeout is set to bigger values (e.g., 1 hour).
     *
     * @param tickTime the min precision for the acknowledgment timeout messages tracker
     * @param timeUnit unit in which the timeout is provided.
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> ackTimeoutTickTime(long tickTime, TimeUnit timeUnit);

    IConsumerBuilder<T> negativeAckRedeliveryDelay(long redeliveryDelay, TimeUnit timeUnit);

    IConsumerBuilder<T> subscriptionType(SubscriptionType subscriptionType);

    IConsumerBuilder<T> subscriptionMode(SubscriptionMode subscriptionMode);

    IConsumerBuilder<T> messageListener(MessageListener<T> messageListener);

    //    /**
    //     * Sets a {@link CryptoKeyReader}.
    //     *
    //     * <p>Configure the key reader to be used to decrypt message payloads.
    //     *
    //     * @param cryptoKeyReader CryptoKeyReader object
    //     * @return the consumer builder instance
    //     */
    //    IConsumerBuilder<T> cryptoKeyReader(CryptoKeyReader cryptoKeyReader);

    /**
     * Sets the default implementation of {@link org.apache.pulsar.client.api.CryptoKeyReader}.
     *
     * <p>Configure the key reader to be used to decrypt message payloads.
     *
     * @param privateKey the private key that is always used to decrypt message payloads.
     * @return the consumer builder instance
     * @since 2.8.0
     */
    IConsumerBuilder<T> defaultCryptoKeyReader(String privateKey);

    /**
     * Sets the default implementation of {@link org.apache.pulsar.client.api.CryptoKeyReader}.
     *
     * <p>Configure the key reader to be used to decrypt the message payloads.
     *
     * @param privateKeys the map of private key names and their URIs used to decrypt message
     *     payloads.
     * @return the consumer builder instance
     * @since 2.8.0
     */
    IConsumerBuilder<T> defaultCryptoKeyReader(Map<String, String> privateKeys);

    //
    //    /**
    //     * Sets a {@link MessageCrypto}.
    //     *
    //     * <p>Contains methods to encrypt/decrypt messages for end-to-end encryption.
    //     *
    //     * @param messageCrypto MessageCrypto object
    //     * @return the consumer builder instance
    //     */
    //    ConsumerBuilder<T> messageCrypto(MessageCrypto messageCrypto);
    //
    //    /**
    //     * Sets the ConsumerCryptoFailureAction to the value specified.
    //     *
    //     * @param action the action the consumer takes in case of decryption failures
    //     * @return the consumer builder instance
    //     */
    //    ConsumerBuilder<T> cryptoFailureAction(ConsumerCryptoFailureAction action);

    /**
     * Sets the size of the consumer receive queue.
     *
     * <p>The consumer receive queue controls how many messages can be accumulated by the {@link
     * org.apache.pulsar.client.api.Consumer} before the application calls {@link
     * org.apache.pulsar.client.api.Consumer#receive()}. Using a higher value can potentially
     * increase consumer throughput at the expense of bigger memory utilization.
     *
     * <p>For the consumer that subscribes to the partitioned topic, the parameter {@link
     * org.apache.pulsar.client.api.ConsumerBuilder#maxTotalReceiverQueueSizeAcrossPartitions} also
     * affects the number of messages accumulated in the consumer.
     *
     * <p><b>Setting the consumer queue size as zero</b>
     *
     * <ul>
     *   <li>Decreases the throughput of the consumer by disabling pre-fetching of messages. This
     *       approach improves the message distribution on shared subscriptions by pushing messages
     *       only to the consumers that are ready to process them. Neither {@link
     *       org.apache.pulsar.client.api.Consumer#receive(int, TimeUnit)} nor Partitioned Topics
     *       can be used if the consumer queue size is zero. {@link
     *       org.apache.pulsar.client.api.Consumer#receive()} function call should not be
     *       interrupted when the consumer queue size is zero.
     *   <li>Doesn't support Batch-Message. If a consumer receives a batch-message, it closes the
     *       consumer connection with the broker and {@link
     *       org.apache.pulsar.client.api.Consumer#receive()} calls remain blocked while {@link
     *       org.apache.pulsar.client.api.Consumer#receiveAsync()} receives exception in callback.
     *       <p><b> The consumer is not able to receive any further messages unless batch-message in
     *       pipeline is removed.</b>
     * </ul>
     *
     * The default value is {@code 1000} messages and should be adequate for most use cases.
     *
     * @param receiverQueueSize the new receiver queue size value
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> receiverQueueSize(int receiverQueueSize);

    /**
     * Sets amount of time for group consumer acknowledgments.
     *
     * <p>By default, the consumer uses a 100 ms grouping time to send out acknowledgments to the
     * broker.
     *
     * <p>Setting a group time of 0 sends out acknowledgments immediately. A longer acknowledgment
     * group time is more efficient, but at the expense of a slight increase in message
     * re-deliveries after a failure.
     *
     * @param delay the max amount of time an acknowledgement can be delayed
     * @param unit the time unit for the delay
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> acknowledgmentGroupTime(long delay, TimeUnit unit);

    /**
     * Set the number of messages for group consumer acknowledgments.
     *
     * <p>By default, the consumer uses at most 1000 messages to send out acknowledgments to the
     * broker.
     *
     * @param messageNum
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> maxAcknowledgmentGroupSize(int messageNum);

    /**
     * @param replicateSubscriptionState
     */
    IConsumerBuilder<T> replicateSubscriptionState(boolean replicateSubscriptionState);

    /**
     * Sets the max total receiver queue size across partitions.
     *
     * <p>This setting is used to reduce the receiver queue size for individual partitions {@link
     * #receiverQueueSize(int)} if the total exceeds this value (default: 50000). The purpose of
     * this setting is to have an upper-limit on the number of messages that a consumer can be
     * pushed at once from a broker, across all the partitions.
     *
     * <p>This setting is applicable only to consumers subscribing to partitioned topics. In such
     * cases, there will be multiple queues for each partition and a single queue for the parent
     * consumer. This setting controls the queues of all partitions, not the parent queue. For
     * instance, if a consumer subscribes to a single partitioned topic, the total number of
     * messages accumulated in this consumer will be the sum of {@link #receiverQueueSize(int)} and
     * maxTotalReceiverQueueSizeAcrossPartitions.
     *
     * @param maxTotalReceiverQueueSizeAcrossPartitions max pending messages across all the
     *     partitions
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> maxTotalReceiverQueueSizeAcrossPartitions(
            int maxTotalReceiverQueueSizeAcrossPartitions);

    /**
     * Sets the consumer name.
     *
     * <p>Consumer names are informative, and can be used to identify a particular consumer instance
     * from the topic stats.
     *
     * @param consumerName
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> consumerName(String consumerName);

    //
    //    /**
    //     * Sets a {@link ConsumerEventListener} for the consumer.
    //     *  TODO: 需要实现
    //     * <p>The consumer group listener is used for receiving consumer state changes in a
    // consumer
    //     * group for failover subscriptions. The application can then react to the consumer state
    //     * changes.
    //     *
    //     * @param consumerEventListener the consumer group listener object
    //     * @return the consumer builder instance
    //     */
    //    ConsumerBuilder<T> consumerEventListener(ConsumerEventListener consumerEventListener);

    /**
     * If enabled, the consumer reads messages from the compacted topic rather than the full message
     * topic backlog. This means that, if the topic has been compacted, the consumer will only see
     * the latest value for each key in the topic, up until the point in the topic message backlog
     * that has been compacted. Beyond that point, the messages are sent as normal.
     *
     * <p>readCompacted can only be enabled on subscriptions to persistent topics with a single
     * active consumer (i.e. failover or exclusive subscriptions). Enabling readCompacted on
     * subscriptions to non-persistent topics or on shared subscriptions will cause the subscription
     * call to throw a PulsarClientException.
     *
     * @param readCompacted whether to read from the compacted topic or full message topic backlog
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> readCompacted(boolean readCompacted);

    /**
     * Sets topic's auto-discovery period when using a pattern for topics consumer. The period is in
     * minutes, and the default and minimum values are 1 minute.
     *
     * @param periodInMinutes number of minutes between checks for new topics matching pattern set
     *     with {@link #topicsPattern(String)}
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> patternAutoDiscoveryPeriod(int periodInMinutes);

    /**
     * Sets topic's auto-discovery period when using a pattern for topics consumer.
     *
     * @param interval the amount of delay between checks for new topics matching pattern set with
     *     {@link #topicsPattern(String)}
     * @param unit the unit of the topics auto discovery period
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> patternAutoDiscoveryPeriod(int interval, TimeUnit unit);

    /**
     * <b>Shared subscription</b>
     *
     * <p>Sets priority level for shared subscription consumers to determine which consumers the
     * broker prioritizes when dispatching messages. Here, the broker follows descending priorities.
     * (eg: 0=max-priority, 1, 2,..)
     *
     * <p>In Shared subscription mode, the broker first dispatches messages to max priority-level
     * consumers if they have permits, otherwise the broker considers next priority level consumers.
     *
     * <p>If a subscription has consumer-A with priorityLevel 0 and Consumer-B with priorityLevel 1,
     * then the broker dispatches messages to only consumer-A until it is drained, and then the
     * broker will start dispatching messages to Consumer-B.
     *
     * <p>
     *
     * <pre>
     * Consumer PriorityLevel Permits
     * C1       0             2
     * C2       0             1
     * C3       0             1
     * C4       1             2
     * C5       1             1
     * Order in which broker dispatches messages to consumers: C1, C2, C3, C1, C4, C5, C4
     * </pre>
     *
     * <p><b>Failover subscription for partitioned topic</b> The broker selects the active consumer
     * for a failover subscription for a partitioned topic based on consumer's priority-level and
     * lexicographical sorting of consumer name. eg:
     *
     * <pre>
     * 1. Active consumer = C1 : Same priority-level and lexicographical sorting
     * Consumer PriorityLevel Name
     * C1       0             aaa
     * C2       0             bbb
     *
     * 2. Active consumer = C2 : Consumer with highest priority
     * Consumer PriorityLevel Name
     * C1       1             aaa
     * C2       0             bbb
     *
     * Partitioned-topics:
     * Broker evenly assigns partitioned topics to highest priority consumers.
     * </pre>
     *
     * <p>Priority level has no effect on failover subscriptions for non-partitioned topics.
     *
     * @param priorityLevel the priority of this consumer
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> priorityLevel(int priorityLevel);

    /**
     * Sets a name/value property with this consumer.
     *
     * <p>Properties are application-defined metadata that can be attached to the consumer. When
     * getting topic stats, this metadata is associated with the consumer stats for easier
     * identification.
     *
     * @param key the property key
     * @param value the property value
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> property(String key, String value);

    /**
     * Add all the properties in the provided map to the consumer.
     *
     * <p>Properties are application-defined metadata that can be attached to the consumer. When
     * getting topic stats, this metadata is associated with the consumer stats for easier
     * identification.
     *
     * @param properties the map with properties
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> properties(Map<String, String> properties);

    /**
     * Sets the {@link org.apache.pulsar.client.api.SubscriptionInitialPosition} for the consumer.
     * TODO: 需要实现
     *
     * @param subscriptionInitialPosition the position where to initialize a newly created
     *     subscription
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> subscriptionInitialPosition(
            SubscriptionInitialPosition subscriptionInitialPosition);

    //    /**
    //     * Determines which topics this consumer should be subscribed to - Persistent,
    // Non-Persistent,
    //     * or both. Only used with pattern subscriptions.
    //     * TODO: 可以延后
    //     * @param regexSubscriptionMode Pattern subscription mode
    //     */
    //    ConsumerBuilder<T> subscriptionTopicsMode(RegexSubscriptionMode regexSubscriptionMode);
    //
    //    /**
    //     * Intercept {@link Consumer}.
    //     * TODO: 需要实现
    //     * @param interceptors the list of interceptors to intercept the consumer created by this
    //     *     builder.
    //     */
    //    ConsumerBuilder<T> intercept(ConsumerInterceptor<T>... interceptors);
    //
    //    /**
    //     * Sets dead letter policy for a consumer.
    //     * TODO: 需要实现
    //     * <p>By default, messages are redelivered as many times as possible until they are
    //     * acknowledged. If you enable a dead letter mechanism, messages will have a
    // maxRedeliverCount.
    //     * When a message exceeds the maximum number of redeliveries, the message is sent to the
    // Dead
    //     * Letter Topic and acknowledged automatically.
    //     *
    //     * <p>Enable the dead letter mechanism by setting dead letter policy. example:
    //     *
    //     * <pre>
    //     * client.newConsumer()
    //     *          .deadLetterPolicy(DeadLetterPolicy.builder().maxRedeliverCount(10).build())
    //     *          .subscribe();
    //     * </pre>
    //     *
    //     * Default dead letter topic name is {TopicName}-{Subscription}-DLQ. To set a custom dead
    // letter
    //     * topic name:
    //     *
    //     * <pre>
    //     * client.newConsumer()
    //     *          .deadLetterPolicy(DeadLetterPolicy
    //     *              .builder()
    //     *              .maxRedeliverCount(10)
    //     *              .deadLetterTopic("your-topic-name")
    //     *              .build())
    //     *          .subscribe();
    //     * </pre>
    //     */
    //    ConsumerBuilder<T> deadLetterPolicy(DeadLetterPolicy deadLetterPolicy);

    /**
     * If enabled, the consumer auto-subscribes for partition increases. This is only for
     * partitioned consumers.
     *
     * @param autoUpdate whether to auto-update partition increases
     */
    IConsumerBuilder<T> autoUpdatePartitions(boolean autoUpdate);

    /**
     * Sets the interval of updating partitions <i>(default: 1 minute)</i>. This only works if
     * autoUpdatePartitions is enabled.
     *
     * @param interval the interval of updating partitions
     * @param unit the time unit of the interval.
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> autoUpdatePartitionsInterval(int interval, TimeUnit unit);

    //    /**
    //     * Sets KeyShared subscription policy for consumer.
    //     *
    //     * <p>By default, KeyShared subscriptions use auto split hash ranges to maintain
    // consumers. If
    //     * you want to set a different KeyShared policy, set a policy by using one of the
    // following
    //     * examples:
    //     *
    //     * <p><b>Sticky hash range policy</b>
    //     *
    //     * <pre>
    //     * client.newConsumer()
    //     *          .keySharedPolicy(KeySharedPolicy.stickyHashRange().ranges(Range.of(0, 10)))
    //     *          .subscribe();
    //     * </pre>
    //     *
    //     * For details about sticky hash range policies, see {@link
    //     * KeySharedPolicy.KeySharedPolicySticky}.
    //     *
    //     * <p><b>Auto-split hash range policy</b>
    //     *
    //     * <pre>
    //     * client.newConsumer()
    //     *          .keySharedPolicy(KeySharedPolicy.autoSplitHashRange())
    //     *          .subscribe();
    //     * </pre>
    //     *
    //     * For details about auto-split hash range policies, see {@link
    //     * KeySharedPolicy.KeySharedPolicyAutoSplit}.
    //     *
    //     * @param keySharedPolicy The {@link KeySharedPolicy} to specify
    //     */
    //    ConsumerBuilder<T> keySharedPolicy(KeySharedPolicy keySharedPolicy);

    /**
     * Sets the consumer to include the given position of any reset operation like {@link
     * org.apache.pulsar.client.api.Consumer#seek(long)} or {@link
     * org.apache.pulsar.client.api.Consumer#seek(org.apache.pulsar.client.api.MessageId)}}.
     *
     * @return the consumer builder instance
     */
    IConsumerBuilder<T> startMessageIdInclusive();

    //
    //    /**
    //     * Sets {@link BatchReceivePolicy} for the consumer. By default, consumer uses {@link
    //     * BatchReceivePolicy#DEFAULT_POLICY} as batch receive policy.
    //     * TODO:可以延后
    //     * <p>Example:
    //     *
    //     * <pre>
    //     * client.newConsumer().batchReceivePolicy(BatchReceivePolicy.builder()
    //     *              .maxNumMessages(100)
    //     *              .maxNumBytes(5 * 1024 * 1024)
    //     *              .timeout(100, TimeUnit.MILLISECONDS)
    //     *              .build()).subscribe();
    //     * </pre>
    //     */
    //    ConsumerBuilder<T> batchReceivePolicy(BatchReceivePolicy batchReceivePolicy);

    /**
     * If enabled, the consumer auto-retries messages. Default: disabled.
     *
     * @param retryEnable whether to auto retry message
     */
    IConsumerBuilder<T> enableRetry(boolean retryEnable);

    /**
     * Enable or disable batch index acknowledgment. To enable this feature, ensure batch index
     * acknowledgment is enabled on the broker side.
     */
    IConsumerBuilder<T> enableBatchIndexAcknowledgment(boolean batchIndexAcknowledgmentEnabled);

    /**
     * Consumer buffers chunk messages into memory until it receives all the chunks of the original
     * message. While consuming chunk-messages, chunks from same message might not be contiguous in
     * the stream and they might be mixed with other messages' chunks. so, consumer has to maintain
     * multiple buffers to manage chunks coming from different messages. This mainly happens when
     * multiple publishers are publishing messages on the topic concurrently or publisher failed to
     * publish all chunks of the messages.
     *
     * <pre>
     * eg: M1-C1, M2-C1, M1-C2, M2-C2
     * Here, Messages M1-C1 and M1-C2 belong to original message M1, M2-C1 and M2-C2 messages belong to M2 message.
     * </pre>
     *
     * Buffering large number of outstanding uncompleted chunked messages can create memory pressure
     * and it can be guarded by providing this @maxPendingChunkedMessage threshold. Once, consumer
     * reaches this threshold, it drops the outstanding unchunked-messages by silently acking or
     * asking broker to redeliver later by marking it unacked. This behavior can be controlled by
     * configuration: @autoAckOldestChunkedMessageOnQueueFull
     *
     * <p>The default value is 10.
     *
     * @param maxPendingChunkedMessage
     * @return
     */
    IConsumerBuilder<T> maxPendingChunkedMessage(int maxPendingChunkedMessage);

    /**
     * Buffering large number of outstanding uncompleted chunked messages can create memory pressure
     * and it can be guarded by providing this @maxPendingChunkedMessage threshold. Once the
     * consumer reaches this threshold, it drops the outstanding unchunked-messages by silently
     * acknowledging if autoAckOldestChunkedMessageOnQueueFull is true, otherwise it marks them for
     * redelivery.
     *
     * @default false
     * @param autoAckOldestChunkedMessageOnQueueFull
     * @return
     */
    IConsumerBuilder<T> autoAckOldestChunkedMessageOnQueueFull(
            boolean autoAckOldestChunkedMessageOnQueueFull);

    /**
     * If the producer fails to publish all the chunks of a message, then the consumer can expire
     * incomplete chunks if the consumer doesn't receive all chunks during the expiration period
     * (default 1 minute).
     *
     * @param duration
     * @param unit
     * @return
     */
    IConsumerBuilder<T> expireTimeOfIncompleteChunkedMessage(long duration, TimeUnit unit);

    /**
     * Enable pooling of messages and the underlying data buffers.
     *
     * <p>When pooling is enabled, the application is responsible for calling Message.release()
     * after the handling of every received message. If “release()” is not called on a received
     * message, it causes a memory leak. If an application attempts to use an already “released”
     * message, it might experience undefined behavior (eg: memory corruption, deserialization
     * error, etc.).
     */
    IConsumerBuilder<T> poolMessages(boolean poolMessages);

    //    /**
    //     * If configured with a non-null value, the consumer uses the processor to process the
    // payload,
    //     * including decoding it to messages and triggering the listener.
    //     * TODO:可以延后
    //     * <p>Default: null
    //     */
    //    ConsumerBuilder<T> messagePayloadProcessor(MessagePayloadProcessor payloadProcessor);
    //
    //    /**
    //     * negativeAckRedeliveryBackoff doesn't work with `consumer.negativeAcknowledge(MessageId
    //     * messageId)` because we are unable to get the redelivery count from the message ID.
    //     * NOTE: 没看懂
    //     * <p>Example:
    //     *
    //     * <pre>
    //     *
    // client.newConsumer().negativeAckRedeliveryBackoff(ExponentialRedeliveryBackoff.builder()
    //     *              .minNackTimeMs(1000)
    //     *              .maxNackTimeMs(60 * 1000)
    //     *              .build()).subscribe();
    //     * </pre>
    //     */
    //    ConsumerBuilder<T> negativeAckRedeliveryBackoff(RedeliveryBackoff
    // negativeAckRedeliveryBackoff);
    //
    //    /**
    //     * redeliveryBackoff doesn't work with `consumer.negativeAcknowledge(MessageId messageId)`
    //     * because we are unable to get the redelivery count from the message ID.
    //     *
    //     * <p>Example:
    //     *
    //     * <pre>
    //     * client.newConsumer().ackTimeout(10, TimeUnit.SECOND)
    //     *              .ackTimeoutRedeliveryBackoff(ExponentialRedeliveryBackoff.builder()
    //     *              .minNackTimeMs(1000)
    //     *              .maxNackTimeMs(60 * 1000)
    //     *              .build()).subscribe();
    //     * </pre>
    //     */
    //    ConsumerBuilder<T> ackTimeoutRedeliveryBackoff(RedeliveryBackoff
    // ackTimeoutRedeliveryBackoff);
    //
    /**
     * Starts the consumer in a paused state. When enabled, the consumer does not immediately fetch
     * messages when {@link #subscribe()} is called. Instead, the consumer waits to fetch messages
     * until {@link org.apache.pulsar.client.api.Consumer#resume()} is called.
     *
     * <p>See also {@link org.apache.pulsar.client.api.Consumer#pause()}.
     *
     * @default false
     */
    IConsumerBuilder<T> startPaused(boolean paused);

    /**
     * If this is enabled, the consumer receiver queue size is initialized as a very small value, 1
     * by default, and will double itself until it reaches the value set by {@link
     * #receiverQueueSize(int)}, if and only if:
     *
     * <p>1) User calls receive() and there are no messages in receiver queue.
     *
     * <p>2) The last message we put in the receiver queue took the last space available in receiver
     * queue.
     *
     * <p>This is disabled by default and currentReceiverQueueSize is initialized as
     * maxReceiverQueueSize.
     *
     * <p>The feature should be able to reduce client memory usage.
     *
     * @param enabled whether to enable AutoScaledReceiverQueueSize.
     */
    IConsumerBuilder<T> autoScaledReceiverQueueSizeEnabled(boolean enabled);

    //    /**
    //     * Configure topic specific options to override those set at the {@link ConsumerBuilder}
    // level.
    //     * TODO: 可以延后
    //     * @param topicName a topic name
    //     * @return a {@link TopicConsumerBuilder} instance
    //     */
    //    TopicConsumerBuilder<T> topicConfiguration(String topicName);
    //
    //    /**
    //     * Configure topic specific options to override those set at the {@link ConsumerBuilder}
    // level.
    //     *
    //     * @param topicName a topic name
    //     * @param builderConsumer a consumer to allow the configuration of the {@link
    //     *     TopicConsumerBuilder} instance
    //     */
    //    ConsumerBuilder<T> topicConfiguration(
    //            String topicName, java.util.function.Consumer<TopicConsumerBuilder<T>>
    // builderConsumer);
    //
    //    /**
    //     * Configure topic specific options to override those set at the {@link ConsumerBuilder}
    // level.
    //     *
    //     * @param topicsPattern a regular expression to match a topic name
    //     * @return a {@link TopicConsumerBuilder} instance
    //     */
    //    TopicConsumerBuilder<T> topicConfiguration(Pattern topicsPattern);
    //
    //    /**
    //     * Configure topic specific options to override those set at the {@link ConsumerBuilder}
    // level.
    //     *
    //     * @param topicsPattern a regular expression to match a topic name
    //     * @param builderConsumer a consumer to allow the configuration of the {@link
    //     *     TopicConsumerBuilder} instance
    //     */
    //    ConsumerBuilder<T> topicConfiguration(
    //            Pattern topicsPattern,
    //            java.util.function.Consumer<TopicConsumerBuilder<T>> builderConsumer);
}
