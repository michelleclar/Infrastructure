package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.carl.infra.mq.common.ex.ProducerException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.model.MessageBuilder;
import org.carl.infra.mq.producer.IProducer;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PulsarProducer<T> implements IProducer<T> {

    private final Producer<T> producer;
    private final MQConfig.ProducerConfig config;

    PulsarProducer(Producer<T> producer, MQConfig.ProducerConfig config) {
        this.producer = producer;
        this.config = config;
    }

    @Override
    public MQConfig.ProducerConfig config() {
        return config;
    }

    @Override
    public SendResult<T> sendMessage(T value) throws ProducerException {
        return this.sendMessage(value, messageBuilder -> {});
    }

    @Override
    public SendResult<T> sendMessage(T value, SendCallback<T> callback) {
        return this.sendMessage(value, messageBuilder -> {}, callback);
    }

    @Override
    public SendResult<T> sendMessage(T value, Consumer<MessageBuilder<T>> consumer)
            throws ProducerException {
        PulsarMessageBuilder<T> msg = new PulsarMessageBuilder<>(value);
        consumer.accept(msg);
        TypedMessageBuilder<T> tTypedMessageBuilder = transfer(msg);
        MessageId send;
        try {
            send = tTypedMessageBuilder.send();
        } catch (PulsarClientException e) {
            throw new ProducerException(e);
        }
        msg.messageId(PulsarMessageIdCodec.encode(send));
        return wrapperSuccess(msg.build());
    }

    @Override
    public SendResult<T> sendMessage(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback) {
        try {
            SendResult<T> tSendResult = sendMessage(value, consumer);
            callback.onSuccess(tSendResult);
            return tSendResult;
        } catch (ProducerException e) {
            callback.onFailure(e);
            return wrapperFailure(e, new PulsarMessageBuilder<T>(value).build());
        }
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(T value) {
        return this.sendMessageAsync(value, messageBuilder -> {});
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(T value, SendCallback<T> callback) {
        return this.sendMessageAsync(value, messageBuilder -> {}, callback);
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(
            T value, Consumer<MessageBuilder<T>> consumer) {
        PulsarMessageBuilder<T> msg = new PulsarMessageBuilder<>(value);
        consumer.accept(msg);
        TypedMessageBuilder<T> tTypedMessageBuilder = transfer(msg);
        CompletableFuture<MessageId> messageIdCompletableFuture = tTypedMessageBuilder.sendAsync();
        return messageIdCompletableFuture.thenApply(
                id -> {
                    msg.messageId(PulsarMessageIdCodec.encode(id));
                    return wrapperSuccess(msg.build());
                });
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback) {
        return this.sendMessageAsync(value, consumer)
                .whenComplete(
                        (tSendResult, ex) -> {
                            if (ex != null) {
                                callback.onFailure(ex);
                                return;
                            }
                            callback.onSuccess(tSendResult);
                        });
    }

    @Override
    public void sendMessages(List<MessageBuilder<T>> messages) {
        sendMessagesAsync(messages).join();
    }

    @Override
    public void sendMessages(List<MessageBuilder<T>> messages, BatchSendCallback<T> callback) {
        sendMessagesAsync(messages)
                .whenComplete(
                        (results, error) -> {
                            if (error != null) {
                                callback.onFailure(error);
                                return;
                            }
                            long successCount = results.stream().filter(SendResult::isSuccess).count();
                            if (successCount == results.size()) {
                                callback.onSuccess(results);
                            } else if (successCount == 0) {
                                callback.onFailure(
                                        new ProducerException("All messages in the batch failed"));
                            } else {
                                callback.onPartialSuccess(results);
                            }
                        });
    }

    @Override
    public CompletableFuture<List<SendResult<T>>> sendMessagesAsync(
            List<MessageBuilder<T>> messages) {
        List<CompletableFuture<SendResult<T>>> futures = new ArrayList<>(messages.size());
        for (MessageBuilder<T> message : messages) {
            CompletableFuture<SendResult<T>> future;
            try {
                future =
                        transfer(message)
                                .sendAsync()
                                .handle(
                                        (messageId, error) -> {
                                            if (error != null) {
                                                return wrapperFailure(error, message.build());
                                            }
                                            message.messageId(PulsarMessageIdCodec.encode(messageId));
                                            return wrapperSuccess(message.build());
                                        });
            } catch (RuntimeException error) {
                future = CompletableFuture.completedFuture(wrapperFailure(error, message.build()));
            }
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    @Override
    public void sendDelayedMessage(MessageBuilder<T> message, long delayMillis) {
        message.deliverAfter(delayMillis);
        try {
            MessageId messageId = transfer(message).send();
            message.messageId(PulsarMessageIdCodec.encode(messageId));
        } catch (PulsarClientException error) {
            throw new RuntimeException(new ProducerException(error));
        }
    }

    @Override
    public void sendDelayedMessage(
            MessageBuilder<T> message, long delayMillis, SendCallback<T> callback) {
        message.deliverAfter(delayMillis);
        transfer(message)
                .sendAsync()
                .whenComplete(
                        (messageId, error) -> {
                            if (error != null) {
                                callback.onFailure(error);
                                return;
                            }
                            message.messageId(PulsarMessageIdCodec.encode(messageId));
                            callback.onSuccess(wrapperSuccess(message.build()));
                        });
    }

    @Override
    public void sendMessageInTransaction(MessageBuilder<T> message) throws ProducerException {
        throw new ProducerException("A Pulsar transaction is required");
    }

    @Override
    public void sendMessageInTransaction(MessageBuilder<T> message, Object transaction)
            throws ProducerException {
        Transaction pulsarTransaction = requireTransaction(transaction);
        try {
            MessageId messageId = transfer(message, pulsarTransaction).send();
            message.messageId(PulsarMessageIdCodec.encode(messageId));
        } catch (PulsarClientException error) {
            throw new ProducerException(error);
        }
    }

    @Override
    public void sendMessageInTransactionAsync(MessageBuilder<T> message) throws ProducerException {
        throw new ProducerException("A Pulsar transaction is required");
    }

    @Override
    public void sendMessageInTransactionAsync(MessageBuilder<T> message, Object transaction)
            throws ProducerException {
        Transaction pulsarTransaction = requireTransaction(transaction);
        transfer(message, pulsarTransaction)
                .sendAsync()
                .thenAccept(messageId -> message.messageId(PulsarMessageIdCodec.encode(messageId)));
    }

    @Override
    public void flush() throws ProducerException {
        try {
            producer.flush();
        } catch (PulsarClientException e) {
            throw new ProducerException(e);
        }
    }

    @Override
    public CompletableFuture<Void> flushAsync() {
        return producer.flushAsync();
    }

    @Override
    @Deprecated
    public ProducerStats getStats() {
        org.apache.pulsar.client.api.ProducerStats stats = producer.getStats();
        return new ProducerStats() {
            @Override
            public long getTotalSentMessages() {
                return stats.getTotalMsgsSent();
            }

            @Override
            public long getTotalSentBytes() {
                return stats.getTotalBytesSent();
            }

            @Override
            public long getTotalSendFailures() {
                return stats.getTotalSendFailed();
            }

            @Override
            public double getAverageSendLatency() {
                return (stats.getSendLatencyMillis50pct() * 0.5
                        + stats.getSendLatencyMillis75pct() * 0.2
                        + stats.getSendLatencyMillis95pct() * 0.2
                        + stats.getSendLatencyMillis99pct() * 0.1);
            }

            @Override
            public int getPendingMessages() {
                return stats.getPendingQueueSize();
            }
        };
    }

    @Override
    public boolean isConnected() {
        return producer.isConnected();
    }

    @Override
    public String getProducerName() {
        return producer.getProducerName();
    }

    @Override
    public void close() throws ProducerException {
        try {
            producer.close();
        } catch (PulsarClientException e) {
            throw new ProducerException(e);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return producer.closeAsync();
    }

    private TypedMessageBuilder<T> transfer(MessageBuilder<T> message) {
        return transfer(message, null);
    }

    private TypedMessageBuilder<T> transfer(MessageBuilder<T> message, Transaction transaction) {
        TypedMessageBuilder<T> tTypedMessageBuilder =
                (transaction == null ? producer.newMessage() : producer.newMessage(transaction))
                        .value(message.getValue());
        message.topic(producer.getTopic());
        if (message.hasDeliverAfter()) {
            tTypedMessageBuilder.deliverAfter(message.getDeliverAfter(), TimeUnit.MILLISECONDS);
        }
        if (message.hasDeliverAt()) {
            tTypedMessageBuilder.deliverAt(message.getDeliverAt());
        }
        if (message.hasProperties()) {
            tTypedMessageBuilder.properties(message.getProperties());
        }
        if (message.isReplicationDisabled()) {
            tTypedMessageBuilder.disableReplication();
        }
        if (message.hasKey()) {
            tTypedMessageBuilder.key(message.getKey());
        }
        if (message.hasSequenceId()) {
            tTypedMessageBuilder.sequenceId(message.getSequenceId());
        }
        if (message.hasEventTime()) {
            tTypedMessageBuilder.eventTime(message.getEventTime());
        }
        return tTypedMessageBuilder;
    }

    private Transaction requireTransaction(Object transaction) throws ProducerException {
        if (transaction instanceof Transaction pulsarTransaction) {
            return pulsarTransaction;
        }
        throw new ProducerException(
                "Transaction must implement org.apache.pulsar.client.api.transaction.Transaction");
    }

    private SendResult<T> wrapperSuccess(Message<T> message) {
        return new SendResult<>() {

            @Override
            public Message<T> getMessage() {
                return message;
            }

            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public String getErrorMessage() {
                return "";
            }
        };
    }

    private SendResult<T> wrapperFailure(Throwable ex, Message<T> message) {
        return new SendResult<>() {

            @Override
            public Message<T> getMessage() {
                return message;
            }

            @Override
            public boolean isSuccess() {
                return false;
            }

            @Override
            public String getErrorMessage() {
                return ex.getMessage();
            }
        };
    }
}
