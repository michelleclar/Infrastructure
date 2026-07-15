package org.carl.infra.mq.kafka.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.common.ex.ConsumerException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.ConsumerStats;
import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.consumer.MessageListener;
import org.carl.infra.mq.model.Message;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class KafkaConsumer<T> implements IConsumer<T>, AutoCloseable {

    private static final ILogger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final KafkaMQClient client;
    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kafkaConsumer;
    private final Class<T> clazz;
    private final MQConfig.ConsumerConfig config;
    private final MessageListener<T> listener;
    private final String subscriptionName;
    private final boolean autoAck;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Queue<Message<T>> recordBuffer = new ConcurrentLinkedQueue<>();
    private final Thread listenerThread;

    private volatile boolean closed = false;
    private volatile boolean paused = false;

    public KafkaConsumer(
            KafkaMQClient client,
            org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kafkaConsumer,
            Class<T> clazz,
            MQConfig.ConsumerConfig config,
            MessageListener<T> listener,
            String subscriptionName,
            boolean autoAck,
            boolean startPaused) {
        this.client = client;
        this.kafkaConsumer = kafkaConsumer;
        this.clazz = clazz;
        this.config = config;
        this.listener = listener;
        this.subscriptionName = subscriptionName;
        this.autoAck = autoAck;
        this.paused = startPaused;

        if (startPaused) {
            pause();
        }

        if (listener != null) {
            this.listenerThread = new Thread(this::runListenerLoop, "kafka-consumer-listener-" + subscriptionName);
            this.listenerThread.setDaemon(true);
            this.listenerThread.start();
        } else {
            this.listenerThread = null;
        }
    }

    private void checkClosed() throws ConsumerException {
        if (closed) {
            throw new ConsumerException("Consumer is closed");
        }
    }

    private void runListenerLoop() {
        while (!closed) {
            try {
                if (paused) {
                    synchronized (kafkaConsumer) {
                        if (kafkaConsumer.assignment().isEmpty()) {
                            kafkaConsumer.poll(Duration.ZERO);
                        } else {
                            kafkaConsumer.pause(kafkaConsumer.assignment());
                        }
                    }
                    Thread.sleep(100);
                    continue;
                }
                ConsumerRecords<String, byte[]> records;
                synchronized (kafkaConsumer) {
                    records = kafkaConsumer.poll(Duration.ofMillis(100));
                }
                if (records != null && !records.isEmpty()) {
                    for (ConsumerRecord<String, byte[]> record : records) {
                        if (closed) {
                            break;
                        }
                        Message<T> msg = convertToMessage(record);
                        try {
                            listener.received(this, msg);
                            if (autoAck) {
                                acknowledge(msg);
                            }
                        } catch (Exception e) {
                            log.error("Error in message listener for record {}: {}", record, e.getMessage(), e);
                            try {
                                listener.onException(this, e);
                            } catch (Exception ex) {
                                log.error("Error invoking listener.onException", ex);
                            }
                            negativeAcknowledge(msg);
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in listener loop: {}", e.getMessage(), e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void pollAndBuffer(Duration timeout) throws ConsumerException {
        ConsumerRecords<String, byte[]> records;
        synchronized (kafkaConsumer) {
            try {
                records = kafkaConsumer.poll(timeout);
            } catch (Exception e) {
                throw new ConsumerException("Failed to poll records from Kafka", e);
            }
        }
        if (records != null && !records.isEmpty()) {
            for (ConsumerRecord<String, byte[]> record : records) {
                recordBuffer.offer(convertToMessage(record));
            }
        }
    }

    private Message<T> convertToMessage(ConsumerRecord<String, byte[]> record) {
        T value = deserialize(record.value());
        Map<String, String> properties = new HashMap<>();
        record.headers().forEach(header -> {
            byte[] headerVal = header.value();
            properties.put(header.key(), headerVal != null ? new String(headerVal, StandardCharsets.UTF_8) : null);
        });

        String messageId = record.topic() + "-" + record.partition() + "-" + record.offset();
        KafkaMessage<T> msg = new KafkaMessage<>(
                value,
                record.key(),
                properties,
                record.timestamp(),
                record.offset(),
                messageId,
                record.topic()
        );
        msg.setSourceMessage(record);
        return msg;
    }

    @SuppressWarnings("unchecked")
    private T deserialize(byte[] data) {
        if (data == null) {
            return null;
        }
        if (clazz.equals(byte[].class)) {
            return (T) data;
        }
        if (clazz.equals(String.class)) {
            return (T) new String(data, StandardCharsets.UTF_8);
        }
        try {
            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize payload for class {}", clazz.getName(), e);
            throw new RuntimeException("Failed to deserialize payload", e);
        }
    }

    @Override
    public Message<T> receive() throws ConsumerException {
        checkClosed();
        while (true) {
            Message<T> msg = recordBuffer.poll();
            if (msg != null) {
                return msg;
            }
            pollAndBuffer(Duration.ofMillis(100));
        }
    }

    @Override
    public Message<T> receive(int timeout, TimeUnit unit) throws ConsumerException {
        checkClosed();
        long timeoutMs = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            Message<T> msg = recordBuffer.poll();
            if (msg != null) {
                return msg;
            }
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return null;
            }
            pollAndBuffer(Duration.ofMillis(Math.min(100, remaining)));
        }
    }

    @Override
    public CompletableFuture<Message<T>> receiveAsync() throws ConsumerException {
        checkClosed();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return receive();
            } catch (ConsumerException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void acknowledge(Message<T> message) throws ConsumerException {
        checkClosed();
        if (message == null) {
            return;
        }
        Object src = message.getSourceMessage();
        if (!(src instanceof ConsumerRecord<?, ?> record)) {
            throw new ConsumerException("Message source is not a ConsumerRecord");
        }
        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
        OffsetAndMetadata om = new OffsetAndMetadata(record.offset() + 1);
        synchronized (kafkaConsumer) {
            try {
                kafkaConsumer.commitSync(Map.of(tp, om));
            } catch (Exception e) {
                throw new ConsumerException("Failed to commit offset sync", e);
            }
        }
    }

    @Override
    public CompletableFuture<Void> acknowledgeAsync(Message<T> message) {
        if (closed) {
            return CompletableFuture.failedFuture(new ConsumerException("Consumer is closed"));
        }
        if (message == null) {
            return CompletableFuture.completedFuture(null);
        }
        Object src = message.getSourceMessage();
        if (!(src instanceof ConsumerRecord<?, ?> record)) {
            return CompletableFuture.failedFuture(new ConsumerException("Message source is not a ConsumerRecord"));
        }
        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
        OffsetAndMetadata om = new OffsetAndMetadata(record.offset() + 1);
        CompletableFuture<Void> future = new CompletableFuture<>();
        synchronized (kafkaConsumer) {
            try {
                kafkaConsumer.commitAsync(Map.of(tp, om), (offsets, exception) -> {
                    if (exception != null) {
                        future.completeExceptionally(new ConsumerException("Failed to commit offset async", exception));
                    } else {
                        future.complete(null);
                    }
                });
            } catch (Exception e) {
                future.completeExceptionally(new ConsumerException(e));
            }
        }
        return future;
    }

    @Override
    public void acknowledgeCumulative(Message<T> message) throws ConsumerException {
        acknowledge(message);
    }

    @Override
    public CompletableFuture<Void> acknowledgeCumulativeAsync(Message<T> message) {
        return acknowledgeAsync(message);
    }

    @Override
    public void negativeAcknowledge(Message<T> message) {
        if (message == null) {
            return;
        }
        Object src = message.getSourceMessage();
        if (!(src instanceof ConsumerRecord<?, ?> record)) {
            log.error("Cannot negative acknowledge message: source is not a ConsumerRecord");
            return;
        }
        TopicPartition tp = new TopicPartition(record.topic(), record.partition());
        synchronized (kafkaConsumer) {
            try {
                kafkaConsumer.seek(tp, record.offset());
            } catch (Exception e) {
                log.error("Failed to seek to offset {} for partition {}", record.offset(), tp, e);
            }
        }
        recordBuffer.clear();
    }

    @Override
    public void pause() {
        synchronized (kafkaConsumer) {
            paused = true;
            try {
                Set<TopicPartition> assignment = kafkaConsumer.assignment();
                if (!assignment.isEmpty()) {
                    kafkaConsumer.pause(assignment);
                }
            } catch (Exception e) {
                log.error("Failed to pause Kafka consumer partitions", e);
            }
        }
    }

    @Override
    public void resume() {
        synchronized (kafkaConsumer) {
            paused = false;
            try {
                Set<TopicPartition> assignment = kafkaConsumer.assignment();
                if (!assignment.isEmpty()) {
                    kafkaConsumer.resume(assignment);
                }
            } catch (Exception e) {
                log.error("Failed to resume Kafka consumer partitions", e);
            }
        }
    }

    @Override
    public void seek(long timestamp) throws ConsumerException {
        checkClosed();
        synchronized (kafkaConsumer) {
            try {
                Set<TopicPartition> assignment = kafkaConsumer.assignment();
                Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
                for (TopicPartition tp : assignment) {
                    timestampsToSearch.put(tp, timestamp);
                }
                Map<TopicPartition, OffsetAndTimestamp> offsets = kafkaConsumer.offsetsForTimes(timestampsToSearch);
                for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : offsets.entrySet()) {
                    if (entry.getValue() != null) {
                        kafkaConsumer.seek(entry.getKey(), entry.getValue().offset());
                    }
                }
                recordBuffer.clear();
            } catch (Exception e) {
                throw new ConsumerException("Failed to seek to timestamp " + timestamp, e);
            }
        }
    }

    @Override
    public void seek(String messageId) throws ConsumerException {
        checkClosed();
        synchronized (kafkaConsumer) {
            try {
                int lastDash = messageId.lastIndexOf('-');
                if (lastDash > 0) {
                    int secondLastDash = messageId.lastIndexOf('-', lastDash - 1);
                    if (secondLastDash > 0) {
                        String topic = messageId.substring(0, secondLastDash);
                        int partition = Integer.parseInt(messageId.substring(secondLastDash + 1, lastDash));
                        long offset = Long.parseLong(messageId.substring(lastDash + 1));
                        TopicPartition tp = new TopicPartition(topic, partition);
                        kafkaConsumer.seek(tp, offset);
                        recordBuffer.clear();
                        return;
                    }
                }
                long offset = Long.parseLong(messageId);
                for (TopicPartition tp : kafkaConsumer.assignment()) {
                    kafkaConsumer.seek(tp, offset);
                }
                recordBuffer.clear();
            } catch (Exception e) {
                throw new ConsumerException("Failed to seek to messageId " + messageId, e);
            }
        }
    }

    @Override
    public ConsumerStats getStats() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return !closed;
    }

    @Override
    public void close() throws ConsumerException {
        if (closed) {
            return;
        }
        closed = true;
        if (listenerThread != null) {
            listenerThread.interrupt();
            try {
                listenerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (kafkaConsumer) {
            try {
                kafkaConsumer.close();
            } catch (Exception e) {
                throw new ConsumerException("Failed to close KafkaConsumer", e);
            }
        }
        if (client != null) {
            client.deregisterResource(this);
        }
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                close();
            } catch (ConsumerException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
