package org.carl.infra.mq.kafka.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.common.ex.ReaderException;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.reader.IReader;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class KafkaReader<T> implements IReader<T> {

    private static final ILogger log = LoggerFactory.getLogger(KafkaReader.class);

    private final KafkaMQClient client;
    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kafkaConsumer;
    private final Class<T> clazz;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Queue<Message<T>> recordBuffer = new ConcurrentLinkedQueue<>();

    private volatile boolean closed = false;

    public KafkaReader(
            KafkaMQClient client,
            org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kafkaConsumer,
            Class<T> clazz) {
        this.client = client;
        this.kafkaConsumer = kafkaConsumer;
        this.clazz = clazz;
    }

    private void checkClosed() throws ReaderException {
        if (closed) {
            throw new ReaderException("Reader is closed");
        }
    }

    private void pollAndBuffer(Duration timeout) throws ReaderException {
        ConsumerRecords<String, byte[]> records;
        synchronized (kafkaConsumer) {
            try {
                records = kafkaConsumer.poll(timeout);
            } catch (Exception e) {
                throw new ReaderException("Failed to poll records in Reader", e);
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
    public Message<T> readNext() throws ReaderException {
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
    public Message<T> readNext(int timeout, TimeUnit unit) throws ReaderException {
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
    public CompletableFuture<Message<T>> readNextAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readNext();
            } catch (ReaderException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean hasMessageAvailable() throws ReaderException {
        checkClosed();
        synchronized (kafkaConsumer) {
            try {
                Set<TopicPartition> partitions = kafkaConsumer.assignment();
                if (partitions.isEmpty()) {
                    return false;
                }
                Map<TopicPartition, Long> endOffsets = kafkaConsumer.endOffsets(partitions);
                for (TopicPartition tp : partitions) {
                    long currentPosition = kafkaConsumer.position(tp);
                    Long endOffset = endOffsets.get(tp);
                    if (endOffset != null && currentPosition < endOffset) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                throw new ReaderException("Failed to check if message is available", e);
            }
        }
    }

    @Override
    public void seek(long timestamp) throws ReaderException {
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
                throw new ReaderException("Failed to seek Reader to timestamp " + timestamp, e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        return !closed;
    }

    @Override
    public void close() throws ReaderException {
        if (closed) {
            return;
        }
        closed = true;
        synchronized (kafkaConsumer) {
            try {
                kafkaConsumer.close();
            } catch (Exception e) {
                throw new ReaderException("Failed to close KafkaConsumer in Reader", e);
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
            } catch (ReaderException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
