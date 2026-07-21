package org.carl.infra.mq.kafka.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.carl.infra.mq.common.ex.ProducerException;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.model.MessageBuilder;
import org.carl.infra.mq.producer.IProducer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class KafkaProducer<T> implements IProducer<T> {

    private final KafkaMQClient client;
    private final String topic;
    private final org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> kafkaProducer;
    private final MQConfig.ProducerConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean closed;

    public KafkaProducer(
            KafkaMQClient client,
            String topic,
            org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> kafkaProducer,
            MQConfig.ProducerConfig config) {
        this.client = client;
        this.topic = topic;
        this.kafkaProducer = kafkaProducer;
        this.config = config;
        if (client != null) {
            client.registerResource(this);
        }
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
    public SendResult<T> sendMessage(T value, SendCallback<T> callback) throws ProducerException {
        return this.sendMessage(value, messageBuilder -> {}, callback);
    }

    @Override
    public SendResult<T> sendMessage(T value, Consumer<MessageBuilder<T>> consumer)
            throws ProducerException {
        KafkaMessageBuilder<T> msg = new KafkaMessageBuilder<>(value);
        consumer.accept(msg);
        if (msg.build().getTopic() == null || msg.build().getTopic().isEmpty()) {
            msg.topic(this.topic);
        }
        byte[] valueBytes = serialize(value);

        ProducerRecord<String, byte[]> record;
        String recordTopic = msg.build().getTopic();
        String recordKey = msg.getKey();
        long timestamp = msg.getEventTime();

        if (timestamp > 0) {
            record = new ProducerRecord<>(recordTopic, null, timestamp, recordKey, valueBytes);
        } else {
            record = new ProducerRecord<>(recordTopic, recordKey, valueBytes);
        }

        if (msg.hasProperties()) {
            for (Map.Entry<String, String> entry : msg.getProperties().entrySet()) {
                record.headers().add(entry.getKey(), entry.getValue() != null ? entry.getValue().getBytes(StandardCharsets.UTF_8) : null);
            }
        }

        try {
            RecordMetadata metadata = kafkaProducer.send(record).get();
            msg.messageId(String.valueOf(metadata.offset()));
            KafkaMessage<T> builtMessage = (KafkaMessage<T>) msg.build();
            builtMessage.setSourceMessage(metadata);
            return wrapperSuccess(builtMessage);
        } catch (Exception e) {
            throw new ProducerException(e);
        }
    }

    @Override
    public SendResult<T> sendMessage(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback) throws ProducerException {
        try {
            SendResult<T> result = sendMessage(value, consumer);
            callback.onSuccess(result);
            return result;
        } catch (Exception e) {
            callback.onFailure(e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(T value) throws ProducerException {
        return this.sendMessageAsync(value, messageBuilder -> {});
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(T value, SendCallback<T> callback)
            throws ProducerException {
        return this.sendMessageAsync(value, messageBuilder -> {}, callback);
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(
            T value, Consumer<MessageBuilder<T>> consumer) throws ProducerException {
        KafkaMessageBuilder<T> msg = new KafkaMessageBuilder<>(value);
        consumer.accept(msg);
        if (msg.build().getTopic() == null || msg.build().getTopic().isEmpty()) {
            msg.topic(this.topic);
        }
        byte[] valueBytes = serialize(value);

        ProducerRecord<String, byte[]> record;
        String recordTopic = msg.build().getTopic();
        String recordKey = msg.getKey();
        long timestamp = msg.getEventTime();

        if (timestamp > 0) {
            record = new ProducerRecord<>(recordTopic, null, timestamp, recordKey, valueBytes);
        } else {
            record = new ProducerRecord<>(recordTopic, recordKey, valueBytes);
        }

        if (msg.hasProperties()) {
            for (Map.Entry<String, String> entry : msg.getProperties().entrySet()) {
                record.headers().add(entry.getKey(), entry.getValue() != null ? entry.getValue().getBytes(StandardCharsets.UTF_8) : null);
            }
        }

        CompletableFuture<SendResult<T>> future = new CompletableFuture<>();
        kafkaProducer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                msg.messageId(String.valueOf(metadata.offset()));
                KafkaMessage<T> builtMessage = (KafkaMessage<T>) msg.build();
                builtMessage.setSourceMessage(metadata);
                future.complete(wrapperSuccess(builtMessage));
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<SendResult<T>> sendMessageAsync(
            T value, Consumer<MessageBuilder<T>> consumer, SendCallback<T> callback) throws ProducerException {
        return this.sendMessageAsync(value, consumer)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        callback.onFailure(ex);
                    } else {
                        callback.onSuccess(result);
                    }
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
    public CompletableFuture<List<SendResult<T>>> sendMessagesAsync(List<MessageBuilder<T>> messages) {
        List<CompletableFuture<SendResult<T>>> futures = new ArrayList<>(messages.size());
        for (MessageBuilder<T> message : messages) {
            if (message.hasDeliverAfter()
                    || message.hasDeliverAt()
                    || message.hasSequenceId()
                    || message.isReplicationDisabled()) {
                futures.add(
                        CompletableFuture.completedFuture(
                                wrapperFailure(
                                        new UnsupportedMQCapabilityException(
                                                "kafka",
                                                "provider-specific batch message metadata",
                                                message),
                                        message.build())));
                continue;
            }
            try {
                futures.add(
                        sendMessageAsync(
                                message.getValue(),
                                builder -> copyPortableMetadata(message, builder))
                                .handle(
                                        (result, error) ->
                                                error == null
                                                        ? result
                                                        : wrapperFailure(error, message.build())));
            } catch (ProducerException error) {
                futures.add(
                        CompletableFuture.completedFuture(
                                wrapperFailure(error, message.build())));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).toList());
    }

    private void copyPortableMetadata(
            MessageBuilder<T> source, MessageBuilder<T> destination) {
        String sourceTopic = source.build().getTopic();
        if (sourceTopic != null) {
            destination.topic(sourceTopic);
        }
        if (source.hasKey()) {
            destination.key(source.getKey());
        }
        if (source.hasProperties()) {
            destination.properties(source.getProperties());
        }
        if (source.hasEventTime()) {
            destination.eventTime(source.getEventTime());
        }
    }

    @Override
    public void sendDelayedMessage(MessageBuilder<T> message, long delayMillis) {
        throw new UnsupportedOperationException("Delayed message not supported yet");
    }

    @Override
    public void sendDelayedMessage(MessageBuilder<T> message, long delayMillis, SendCallback<T> callback) {
        throw new UnsupportedOperationException("Delayed message not supported yet");
    }

    @Override
    public void sendMessageInTransaction(MessageBuilder<T> message) throws ProducerException {
        throw new UnsupportedOperationException("Transactions not supported yet");
    }

    @Override
    public void sendMessageInTransaction(MessageBuilder<T> message, Object transaction) throws ProducerException {
        throw new UnsupportedOperationException("Transactions not supported yet");
    }

    @Override
    public void sendMessageInTransactionAsync(MessageBuilder<T> message) throws ProducerException {
        throw new UnsupportedOperationException("Transactions not supported yet");
    }

    @Override
    public void sendMessageInTransactionAsync(MessageBuilder<T> message, Object transaction) throws ProducerException {
        throw new UnsupportedOperationException("Transactions not supported yet");
    }

    @Override
    public void flush() throws ProducerException {
        kafkaProducer.flush();
    }

    @Override
    public CompletableFuture<Void> flushAsync() {
        return CompletableFuture.runAsync(kafkaProducer::flush);
    }

    @Override
    @Deprecated
    public ProducerStats getStats() {
        throw new UnsupportedMQCapabilityException("kafka", "legacy producer statistics", "getStats");
    }

    @Override
    public boolean isConnected() {
        return !closed;
    }

    @Override
    public String getProducerName() {
        return "kafka-producer-" + topic;
    }

    @Override
    public void close() throws ProducerException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            kafkaProducer.close();
        } finally {
            if (client != null) {
                client.deregisterResource(this);
            }
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                close();
            } catch (ProducerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private byte[] serialize(T value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof String string) {
            return string.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message value", e);
        }
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

    private SendResult<T> wrapperFailure(Throwable error, Message<T> message) {
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
                return error.getMessage();
            }
        };
    }
}
