package org.carl.infrastructure.mq.kafka.builder;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.kafka.config.KafkaConfig;
import org.carl.infrastructure.mq.model.Message;
import org.carl.infrastructure.mq.reader.ReaderStartPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KafkaReaderTest {

    private org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> mockKafkaConsumer;
    private KafkaMQClient client;
    private MQConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        mockKafkaConsumer = mock(org.apache.kafka.clients.consumer.KafkaConsumer.class);
        config = new KafkaConfig("localhost:9092");
        client = new KafkaMQClient(config);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadNext_DeserializationAndBuffering() throws Exception {
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("test-topic", 0, 100L, "key-1", "hello".getBytes(StandardCharsets.UTF_8));
        Map<TopicPartition, List<ConsumerRecord<String, byte[]>>> recordsMap = new HashMap<>();
        recordsMap.put(new TopicPartition("test-topic", 0), Collections.singletonList(record));
        ConsumerRecords<String, byte[]> consumerRecords = new ConsumerRecords<>(recordsMap);

        when(mockKafkaConsumer.poll(any(Duration.class)))
                .thenReturn(consumerRecords)
                .thenReturn(ConsumerRecords.empty());

        KafkaReader<byte[]> reader = new KafkaReader<>(client, mockKafkaConsumer, byte[].class);

        Message<byte[]> msg = reader.readNext();
        assertNotNull(msg);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), msg.getValue());
        assertEquals("key-1", msg.getKey());
        assertEquals("test-topic-0-100", msg.getMessageId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHasMessageAvailable_ReturnsTrueWhenPositionBehindEndOffset() throws Exception {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        Set<TopicPartition> assignment = Collections.singleton(tp);
        when(mockKafkaConsumer.assignment()).thenReturn(assignment);
        when(mockKafkaConsumer.position(tp)).thenReturn(10L);
        when(mockKafkaConsumer.endOffsets(assignment)).thenReturn(Map.of(tp, 15L));

        KafkaReader<byte[]> reader = new KafkaReader<>(client, mockKafkaConsumer, byte[].class);

        assertTrue(reader.hasMessageAvailable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHasMessageAvailable_ReturnsFalseWhenPositionAtEndOffset() throws Exception {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        Set<TopicPartition> assignment = Collections.singleton(tp);
        when(mockKafkaConsumer.assignment()).thenReturn(assignment);
        when(mockKafkaConsumer.position(tp)).thenReturn(15L);
        when(mockKafkaConsumer.endOffsets(assignment)).thenReturn(Map.of(tp, 15L));

        KafkaReader<byte[]> reader = new KafkaReader<>(client, mockKafkaConsumer, byte[].class);

        assertFalse(reader.hasMessageAvailable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSeekTimestamp() throws Exception {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        Set<TopicPartition> assignment = Collections.singleton(tp);
        when(mockKafkaConsumer.assignment()).thenReturn(assignment);

        OffsetAndTimestamp offsetAndTimestamp = new OffsetAndTimestamp(200L, 5000L);
        when(mockKafkaConsumer.offsetsForTimes(anyMap())).thenReturn(Map.of(tp, offsetAndTimestamp));

        KafkaReader<byte[]> reader = new KafkaReader<>(client, mockKafkaConsumer, byte[].class);

        reader.seek(5000L);

        verify(mockKafkaConsumer).seek(tp, 200L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolvePartitions_SingleTopic() throws Exception {
        List<PartitionInfo> infos = Arrays.asList(
                new PartitionInfo("my-topic", 0, null, null, null),
                new PartitionInfo("my-topic", 1, null, null, null)
        );
        when(mockKafkaConsumer.partitionsFor("my-topic")).thenReturn(infos);

        KafkaReaderBuilder<byte[]> builder = new KafkaReaderBuilder<>(client, byte[].class);
        builder.topic("my-topic");

        List<TopicPartition> partitions = builder.resolvePartitions(mockKafkaConsumer);
        assertEquals(2, partitions.size());
        assertEquals("my-topic", partitions.get(0).topic());
        assertEquals(0, partitions.get(0).partition());
        assertEquals("my-topic", partitions.get(1).topic());
        assertEquals(1, partitions.get(1).partition());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolvePartitions_TopicPattern() throws Exception {
        Map<String, List<PartitionInfo>> allTopics = new HashMap<>();
        allTopics.put("persistent://public/default/cdc-events-user", Collections.singletonList(
                new PartitionInfo("persistent://public/default/cdc-events-user", 0, null, null, null)
        ));
        allTopics.put("persistent://public/default/cdc-events-order-partition-0", Collections.singletonList(
                new PartitionInfo("persistent://public/default/cdc-events-order-partition-0", 0, null, null, null)
        ));
        allTopics.put("other-topic", Collections.singletonList(
                new PartitionInfo("other-topic", 0, null, null, null)
        ));

        when(mockKafkaConsumer.listTopics()).thenReturn(allTopics);

        KafkaReaderBuilder<byte[]> builder = new KafkaReaderBuilder<>(client, byte[].class);
        builder.topicsPattern("cdc-events-.*");

        List<TopicPartition> partitions = builder.resolvePartitions(mockKafkaConsumer);
        assertEquals(2, partitions.size());

        Set<String> topicsFound = new HashSet<>();
        for (TopicPartition tp : partitions) {
            topicsFound.add(tp.topic());
        }
        assertTrue(topicsFound.contains("persistent://public/default/cdc-events-user"));
        assertTrue(topicsFound.contains("persistent://public/default/cdc-events-order-partition-0"));
        assertFalse(topicsFound.contains("other-topic"));
    }

    @Test
    public void testCleanTopicName() {
        KafkaReaderBuilder<byte[]> builder = new KafkaReaderBuilder<>(client, byte[].class);
        assertEquals("cdc-events-user", builder.cleanTopicName("persistent://cdc-events-user"));
        assertEquals("cdc-events-user", builder.cleanTopicName("non-persistent://cdc-events-user"));
        assertEquals("cdc-events-order", builder.cleanTopicName("persistent://cdc-events-order-partition-0"));
        assertEquals("other-topic", builder.cleanTopicName("other-topic"));
    }
}
