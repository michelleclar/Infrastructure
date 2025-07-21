package org.carl.infrastructure.pulsar.factory;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.carl.infrastructure.pulsar.config.TimeLimitedConsumerEventListener;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PulsarConsumer<T> implements IConsumer<T>, AutoCloseable {

    private final PulsarClient pulsarClient;

    private final PulsarConsumerBuilder<T> pulsarConsumerBuilder;
    private MessageListener<T> messageListener;
    private final MsgArgsConfig.ConsumerConfig consumerConfig;
    private final String topic;
    private ConsumerBuilder<T> consumerBuilder;
    private final int consumerSize;
    private String subscriptionName;
    private final Map<String, Consumer<T>> subscriptions = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(PulsarConsumer.class);

    public PulsarConsumer(
            PulsarClient pulsarClient,
            MsgArgsConfig.ConsumerConfig consumerConfig,
            String topic,
            Class<T> clazz) {
        this.pulsarClient = pulsarClient;
        this.consumerConfig = consumerConfig;
        this.topic = topic;
        this.pulsarConsumerBuilder = PulsarConsumerBuilder.create(pulsarClient, topic, clazz);
        this.consumerSize = 1;
    }

    public PulsarConsumer(
            PulsarClient pulsarClient,
            MsgArgsConfig.ConsumerConfig consumerConfig,
            String topic,
            Class<T> clazz,
            int consumerSize) {
        this.pulsarClient = pulsarClient;
        this.consumerConfig = consumerConfig;
        this.topic = topic;
        this.pulsarConsumerBuilder = PulsarConsumerBuilder.create(pulsarClient, topic, clazz);
        this.consumerSize = consumerSize;
    }

    @Override
    public IConsumer<T> subscribeName(String subscribeName) {
        this.consumerBuilder = pulsarConsumerBuilder.build(subscribeName);
        this.subscriptionName = subscribeName;
        return this;
    }

    @Override
    public IConsumer<T> subscribe() throws ConsumerException {
        process(consumerSize);
        return this;
    }

    @Override
    public IConsumer<T> subscribe(int consumerSize) throws ConsumerException {
        process(consumerSize);
        return this;
    }

    @Override
    public IConsumer<T> subscribe(int consumerSize, long deliver, TimeUnit unit)
            throws ConsumerException {
        ConsumerBuilder<T> clone = consumerBuilder.clone();
        clone.consumerEventListener(new TimeLimitedConsumerEventListener(deliver, unit));
        process(consumerSize, clone);
        return this;
    }

    @Override
    public MessageBuilder.Message<T> receive() throws ConsumerException {
        try (Consumer<T> subscribe = consumerBuilder.subscribe()) {
            Message<T> receive = subscribe.receive();
            return wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public MessageBuilder.Message<T> receive(long timeout, TimeUnit unit) throws ConsumerException {
        try (Consumer<T> subscribe = consumerBuilder.clone().subscribe()) {
            Message<T> receive = subscribe.receive((int) timeout, unit);
            return wrapper(receive);
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<MessageBuilder.Message<T>> receiveAsync() throws ConsumerException {
        try {

            Consumer<T> subscribe = consumerBuilder.clone().subscribe();
            return subscribe
                    .receiveAsync()
                    .thenApply(this::wrapper)
                    .whenComplete(
                            (r, t) -> {
                                try {
                                    subscribe.close();
                                } catch (PulsarClientException e) {
                                    throw new RuntimeException(e);
                                }
                            });
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public IConsumer<T> setMessageListener(MessageListener<T> listener) {
        this.messageListener = listener;
        //        IConsumer<T> outer = this;
        //        consumerBuilder.messageListener(
        //                new org.apache.pulsar.client.api.MessageListener<>() {
        //                    @Override
        //                    public void received(Consumer<T> consumer, Message<T> msg) {
        //                        try {
        //                            listener.received(outer, wrapper(msg));
        //                            consumer.acknowledge(msg);
        //                        } catch (Exception e) {
        //                            consumer.negativeAcknowledge(msg);
        //                            listener.onException(outer, e);
        //                        }
        //                    }
        //
        //                    @Override
        //                    public void reachedEndOfTopic(Consumer<T> consumer) {
        //
        // org.apache.pulsar.client.api.MessageListener.super.reachedEndOfTopic(
        //                                consumer);
        //                        try {
        //                            listener.reachedEndOfTopic(outer);
        //                        } catch (Exception e) {
        //                            listener.onException(outer, e);
        //                        }
        //                    }
        //                });
        return this;
    }

    @Override
    public List<MessageBuilder.Message<T>> batchReceive(int maxMessages) throws ConsumerException {
        return List.of();
    }

    @Override
    public List<MessageBuilder.Message<T>> batchReceive(
            int maxMessages, long timeout, TimeUnit unit) throws ConsumerException {
        return List.of();
    }

    @Override
    public CompletableFuture<List<MessageBuilder.Message<T>>> batchReceiveAsync(int maxMessages) {
        return null;
    }

    @Override
    public void acknowledge(MessageBuilder.Message<T> message) throws ConsumerException {}

    @Override
    public CompletableFuture<Void> acknowledgeAsync(MessageBuilder.Message<T> message) {
        return null;
    }

    @Override
    public void acknowledge(List<MessageBuilder.Message<T>> messages) throws ConsumerException {}

    @Override
    public CompletableFuture<Void> acknowledgeAsync(List<MessageBuilder.Message<T>> messages) {
        return null;
    }

    @Override
    public void acknowledgeCumulative(MessageBuilder.Message<T> message) throws ConsumerException {}

    @Override
    public CompletableFuture<Void> acknowledgeCumulativeAsync(MessageBuilder.Message<T> message) {
        return null;
    }

    @Override
    public void negativeAcknowledge(MessageBuilder.Message<T> message) {}

    @Override
    public void negativeAcknowledge(List<MessageBuilder.Message<T>> messages) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public void seek(long timestamp) throws ConsumerException {}

    @Override
    public void seek(String messageId) throws ConsumerException {}

    @Override
    public void seekToBeginning() throws ConsumerException {}

    @Override
    public void seekToEnd() throws ConsumerException {}

    @Override
    public ConsumerStats getStats() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return !subscriptions.isEmpty();
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getSubscription() {
        return subscriptionName;
    }

    @Override
    public String getConsumerName() {
        return subscriptionName;
    }

    @Override
    public SubscriptionType getSubscriptionType() {
        return switch (consumerConfig.subscriptionType()) {
            case Failover:
                yield SubscriptionType.FAILOVER;
            case Shared:
                yield SubscriptionType.SHARED;
            case Key_Shared:
                yield SubscriptionType.KEY_SHARED;
            case Exclusive:
                yield SubscriptionType.EXCLUSIVE;
        };
    }

    @Override
    public void close() throws ConsumerException {
        try {
            for (Map.Entry<String, Consumer<T>> entry : subscriptions.entrySet()) {
                Consumer<T> v = entry.getValue();
                v.close();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Close consumer [%s]", entry.getKey());
                }
            }
            subscriptions.clear();
        } catch (PulsarClientException e) {
            throw new ConsumerException(e);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        // 收集所有的 closeAsync 操作
        CompletableFuture<?>[] closeFutures =
                subscriptions.entrySet().stream()
                        .map(
                                entry -> {
                                    Consumer<T> consumer = entry.getValue();
                                    return consumer.closeAsync()
                                            .whenComplete(
                                                    (v, throwable) -> {
                                                        if (throwable != null) {
                                                            LOGGER.errorf(
                                                                    "Close consumer failure [%s]: %s",
                                                                    entry.getKey(),
                                                                    throwable.getMessage());
                                                        } else if (LOGGER.isDebugEnabled()) {
                                                            LOGGER.debugf(
                                                                    "Close consumer [%s]",
                                                                    entry.getKey());
                                                        }
                                                    });
                                })
                        .toArray(CompletableFuture[]::new);

        // 等待所有操作完成
        return CompletableFuture.allOf(closeFutures)
                .whenComplete(
                        (__, ex) -> {
                            subscriptions.clear();
                        });
    }

    private MessageBuilder.Message<T> wrapper(Message<T> message) {
        MessageBuilder<T> value =
                new PulsarMessageBuilder<>(message.getValue())
                        .properties(message.getProperties())
                        .key(message.getKey())
                        .eventTime(message.getEventTime())
                        .topic(message.getTopicName())
                        .sequenceId(message.getSequenceId());
        return value.build();
    }

    private void process(int consumerSize, ConsumerBuilder<T> consumerBuilder)
            throws ConsumerException {
        if (messageListener == null) {
            throw new ConsumerException("messageListener is null");
        }
        IConsumer<T> outer = this;
        consumerBuilder.messageListener(
                new org.apache.pulsar.client.api.MessageListener<>() {
                    @Override
                    public void received(Consumer<T> consumer, Message<T> msg) {
                        try {
                            messageListener.received(outer, wrapper(msg));
                            consumer.acknowledge(msg);
                        } catch (Exception e) {
                            consumer.negativeAcknowledge(msg);
                            messageListener.onException(outer, e);
                        }
                    }

                    @Override
                    public void reachedEndOfTopic(Consumer<T> consumer) {
                        org.apache.pulsar.client.api.MessageListener.super.reachedEndOfTopic(
                                consumer);
                        try {
                            messageListener.reachedEndOfTopic(outer);
                        } catch (Exception e) {
                            messageListener.onException(outer, e);
                        }
                    }
                });
        for (int i = 0; i < consumerSize; i++) {
            try {
                String consumerName = subscriptionName + "-" + i;
                Consumer<T> subscribe =
                        consumerBuilder.consumerName(subscriptionName + "-" + i).subscribe();
                subscriptions.put(consumerName, subscribe);
            } catch (PulsarClientException e) {
                throw new ConsumerException(e);
            }
        }
    }

    private void process(int consumerSize) throws ConsumerException {
        process(consumerSize, this.consumerBuilder.clone());
    }
}
