package org.carl.infrastructure.mq.kafka.builder;

import org.apache.kafka.common.TopicPartition;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.common.ex.ReaderException;
import org.carl.infrastructure.mq.reader.IReader;
import org.carl.infrastructure.mq.reader.IReaderBuilder;
import org.carl.infrastructure.mq.reader.ReaderStartPosition;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class KafkaReaderBuilder<T> implements IReaderBuilder<T> {

    private static final ILogger log = LoggerFactory.getLogger(KafkaReaderBuilder.class);

    private final KafkaMQClient client;
    private final Class<T> clazz;

    private String topic;
    private Pattern topicsPattern;
    private ReaderStartPosition startPosition = ReaderStartPosition.Latest;
    private String readerName;
    private int receiverQueueSize = 1000;

    public static <T> KafkaReaderBuilder<T> create(KafkaMQClient client, Class<T> clazz) {
        return new KafkaReaderBuilder<>(client, clazz);
    }

    public static KafkaReaderBuilder<byte[]> create(KafkaMQClient client) {
        return new KafkaReaderBuilder<>(client, byte[].class);
    }

    public KafkaReaderBuilder(KafkaMQClient client, Class<T> clazz) {
        this.client = client;
        this.clazz = clazz;
    }

    @Override
    public IReaderBuilder<T> topic(String topic) {
        this.topic = topic;
        return this;
    }

    @Override
    public IReaderBuilder<T> topicsPattern(Pattern topicsPattern) {
        this.topicsPattern = topicsPattern;
        return this;
    }

    @Override
    public IReaderBuilder<T> topicsPattern(String topicsPattern) {
        this.topicsPattern = Pattern.compile(topicsPattern);
        return this;
    }

    @Override
    public IReaderBuilder<T> startMessageId(ReaderStartPosition position) {
        this.startPosition = position;
        return this;
    }

    @Override
    public IReaderBuilder<T> readerName(String readerName) {
        this.readerName = readerName;
        return this;
    }

    @Override
    public IReaderBuilder<T> receiverQueueSize(int receiverQueueSize) {
        this.receiverQueueSize = receiverQueueSize;
        return this;
    }

    @Override
    public IReader<T> create() throws ReaderException {
        Map<String, Object> props = buildReaderProperties();
        org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kc = null;
        try {
            kc = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

            List<TopicPartition> partitions = resolvePartitions(kc);
            if (!partitions.isEmpty()) {
                kc.assign(partitions);
                if (startPosition == ReaderStartPosition.Earliest) {
                    kc.seekToBeginning(partitions);
                } else {
                    kc.seekToEnd(partitions);
                }
            }

            KafkaReader<T> reader = new KafkaReader<>(client, kc, clazz);
            client.registerResource(reader);
            return reader;
        } catch (Exception e) {
            if (kc != null) {
                kc.close();
            }
            throw new ReaderException(e);
        }
    }

    @Override
    public IReader<T> create(String topic) throws ReaderException {
        this.topic = topic;
        return create();
    }

    @Override
    public CompletableFuture<IReader<T>> createAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return create();
            } catch (ReaderException e) {
                throw new RuntimeException(e);
            }
        });
    }

    List<TopicPartition> resolvePartitions(org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> kc) throws ReaderException {
        List<TopicPartition> partitions = new ArrayList<>();
        try {
            if (topicsPattern != null) {
                Map<String, List<org.apache.kafka.common.PartitionInfo>> allTopics = kc.listTopics();
                if (allTopics != null) {
                    for (Map.Entry<String, List<org.apache.kafka.common.PartitionInfo>> entry : allTopics.entrySet()) {
                        String topicName = entry.getKey();
                        if (topicsPattern.matcher(cleanTopicName(topicName)).matches()) {
                            List<org.apache.kafka.common.PartitionInfo> infos = entry.getValue();
                            if (infos != null) {
                                for (org.apache.kafka.common.PartitionInfo pi : infos) {
                                    partitions.add(new TopicPartition(pi.topic(), pi.partition()));
                                }
                            }
                        }
                    }
                }
            } else if (topic != null && !topic.isEmpty()) {
                List<org.apache.kafka.common.PartitionInfo> infos = kc.partitionsFor(topic);
                if (infos != null) {
                    for (org.apache.kafka.common.PartitionInfo pi : infos) {
                        partitions.add(new TopicPartition(pi.topic(), pi.partition()));
                    }
                }
            } else {
                throw new ReaderException("No topic or topic pattern configured for Reader");
            }
        } catch (Exception e) {
            throw new ReaderException("Failed to resolve partitions for Reader", e);
        }
        return partitions;
    }

    String cleanTopicName(String tName) {
        if (tName == null) {
            return "";
        }
        if (tName.startsWith("persistent://")) {
            tName = tName.substring("persistent://".length());
        } else if (tName.startsWith("non-persistent://")) {
            tName = tName.substring("non-persistent://".length());
        }
        int pathSeparator = tName.lastIndexOf('/');
        if (pathSeparator >= 0) {
            tName = tName.substring(pathSeparator + 1);
        }
        int partitionIdx = tName.lastIndexOf("-partition-");
        if (partitionIdx > 0) {
            tName = tName.substring(0, partitionIdx);
        }
        return tName;
    }

    private Map<String, Object> buildReaderProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", client.getClientConfig().serviceUrl());
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("enable.auto.commit", "false");

        if (readerName != null) {
            props.put("client.id", readerName);
        }

        return props;
    }
}
