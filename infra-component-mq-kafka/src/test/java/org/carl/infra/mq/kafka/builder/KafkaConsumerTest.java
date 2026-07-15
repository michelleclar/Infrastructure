package org.carl.infra.mq.kafka.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.consumer.MessageListener;
import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.kafka.config.KafkaConfig;
import org.carl.infra.mq.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KafkaConsumerTest {

    private org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]> mockKafkaConsumer;
    private KafkaMQClient client;
    private MQConfig config;

    public static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        mockKafkaConsumer = mock(org.apache.kafka.clients.consumer.KafkaConsumer.class);
        config = new KafkaConfig("localhost:9092");
        client = new KafkaMQClient(config);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReceive_DeserializationAndBuffering() throws Exception {
        TestUser user = new TestUser("Alice", 25);
        byte[] payload = new ObjectMapper().writeValueAsBytes(user);

        ConsumerRecord<String, byte[]> record1 = new ConsumerRecord<>("test-topic", 0, 100L, "key-1", payload);
        ConsumerRecord<String, byte[]> record2 = new ConsumerRecord<>("test-topic", 0, 101L, "key-2", payload);

        Map<TopicPartition, List<ConsumerRecord<String, byte[]>>> recordsMap = new HashMap<>();
        recordsMap.put(new TopicPartition("test-topic", 0), Arrays.asList(record1, record2));
        ConsumerRecords<String, byte[]> consumerRecords = new ConsumerRecords<>(recordsMap);

        when(mockKafkaConsumer.poll(any(Duration.class)))
                .thenReturn(consumerRecords)
                .thenReturn(ConsumerRecords.empty());

        KafkaConsumer<TestUser> consumer = new KafkaConsumer<>(
                client, mockKafkaConsumer, TestUser.class, config.consumer(), null, "sub-name", false, false
        );

        Message<TestUser> msg1 = consumer.receive();
        assertNotNull(msg1);
        assertEquals("Alice", msg1.getValue().getName());
        assertEquals("key-1", msg1.getKey());
        assertEquals("test-topic-0-100", msg1.getMessageId());
        assertEquals(record1, msg1.getSourceMessage());

        Message<TestUser> msg2 = consumer.receive();
        assertNotNull(msg2);
        assertEquals("Alice", msg2.getValue().getName());
        assertEquals("key-2", msg2.getKey());
        assertEquals("test-topic-0-101", msg2.getMessageId());
        assertEquals(record2, msg2.getSourceMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcknowledge_CommitsOffsetPlusOne() throws Exception {
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("test-topic", 2, 50L, "key", "val".getBytes());
        Message<byte[]> message = mock(Message.class);
        when(message.getSourceMessage()).thenReturn(record);

        KafkaConsumer<byte[]> consumer = new KafkaConsumer<>(
                client, mockKafkaConsumer, byte[].class, config.consumer(), null, "sub-name", false, false
        );

        consumer.acknowledge(message);

        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockKafkaConsumer).commitSync(captor.capture());

        Map<TopicPartition, OffsetAndMetadata> committed = captor.getValue();
        assertEquals(1, committed.size());
        TopicPartition tp = new TopicPartition("test-topic", 2);
        assertTrue(committed.containsKey(tp));
        assertEquals(51L, committed.get(tp).offset());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNegativeAcknowledge_SeeksBackToOffset() throws Exception {
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("test-topic", 1, 99L, "key", "val".getBytes());
        Message<byte[]> message = mock(Message.class);
        when(message.getSourceMessage()).thenReturn(record);

        KafkaConsumer<byte[]> consumer = new KafkaConsumer<>(
                client, mockKafkaConsumer, byte[].class, config.consumer(), null, "sub-name", false, false
        );

        consumer.negativeAcknowledge(message);

        verify(mockKafkaConsumer).seek(new TopicPartition("test-topic", 1), 99L);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBackgroundListenerLoop_AutoAck() throws Exception {
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>("test-topic", 0, 10L, "key", "val".getBytes());
        Map<TopicPartition, List<ConsumerRecord<String, byte[]>>> recordsMap = new HashMap<>();
        recordsMap.put(new TopicPartition("test-topic", 0), Collections.singletonList(record));
        ConsumerRecords<String, byte[]> consumerRecords = new ConsumerRecords<>(recordsMap);

        when(mockKafkaConsumer.poll(any(Duration.class)))
                .thenReturn(consumerRecords)
                .thenAnswer(inv -> {
                    Thread.sleep(1000);
                    return ConsumerRecords.empty();
                });

        CompletableFuture<Message<byte[]>> receivedFuture = new CompletableFuture<>();
        MessageListener<byte[]> listener = new MessageListener<>() {
            @Override
            public void received(IConsumer<byte[]> consumer, Message<byte[]> message) {
                receivedFuture.complete(message);
            }
        };

        KafkaConsumer<byte[]> consumer = new KafkaConsumer<>(
                client, mockKafkaConsumer, byte[].class, config.consumer(), listener, "sub-name", true, false
        );

        Message<byte[]> msg = receivedFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(msg);
        assertArrayEquals("val".getBytes(), msg.getValue());

        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockKafkaConsumer, timeout(1000)).commitSync(captor.capture());
        Map<TopicPartition, OffsetAndMetadata> committed = captor.getValue();
        assertEquals(11L, committed.get(new TopicPartition("test-topic", 0)).offset());

        consumer.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSeekTimestamp() throws Exception {
        TopicPartition tp = new TopicPartition("test-topic", 0);
        Set<TopicPartition> assignment = Collections.singleton(tp);
        when(mockKafkaConsumer.assignment()).thenReturn(assignment);

        OffsetAndTimestamp offsetAndTimestamp = new OffsetAndTimestamp(500L, 1000L);
        Map<TopicPartition, OffsetAndTimestamp> offsets = Map.of(tp, offsetAndTimestamp);
        when(mockKafkaConsumer.offsetsForTimes(anyMap())).thenReturn(offsets);

        KafkaConsumer<byte[]> consumer = new KafkaConsumer<>(
                client, mockKafkaConsumer, byte[].class, config.consumer(), null, "sub-name", false, false
        );

        consumer.seek(1000L);

        verify(mockKafkaConsumer).seek(tp, 500L);
    }
}
