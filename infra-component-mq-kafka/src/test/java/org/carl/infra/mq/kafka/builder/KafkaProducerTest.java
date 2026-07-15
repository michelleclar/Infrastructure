package org.carl.infra.mq.kafka.builder;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.kafka.config.KafkaConfig;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.producer.IProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class KafkaProducerTest {

    private org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> mockKafkaProducer;
    private KafkaMQClient client;
    private KafkaProducer<TestUser> producer;

    public static class TestUser {
        private String name;
        private int age;

        public TestUser() {}

        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        mockKafkaProducer = mock(org.apache.kafka.clients.producer.KafkaProducer.class);

        MQConfig config = new KafkaConfig("localhost:9092");
        client = new KafkaMQClient(config);

        // Mock send future and metadata
        Future<RecordMetadata> mockFuture = mock(Future.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("test-topic", 0), 0L, 123L, 0L, 0L, 0, 0);
        when(mockFuture.get()).thenReturn(metadata);

        when(mockKafkaProducer.send(any(ProducerRecord.class))).thenReturn(mockFuture);

        when(mockKafkaProducer.send(any(ProducerRecord.class), any(Callback.class))).thenAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(metadata, null);
            return mockFuture;
        });

        producer = new KafkaProducer<>(client, "test-topic", mockKafkaProducer, config.producer());
    }

    @Test
    public void testSendMessage_ByteArraysPassedThrough() throws Exception {
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        KafkaProducer<byte[]> bytesProducer = new KafkaProducer<>(client, "test-topic", (org.apache.kafka.clients.producer.KafkaProducer<String, byte[]>) (Object) mockKafkaProducer, client.newProducer().create("test-topic").config());

        bytesProducer.sendMessage(payload);

        ArgumentCaptor<ProducerRecord<String, byte[]>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockKafkaProducer).send(captor.capture());

        ProducerRecord<String, byte[]> record = captor.getValue();
        assertArrayEquals(payload, record.value());
    }

    @Test
    public void testSendMessage_ObjectSerializedWithJackson() throws Exception {
        TestUser user = new TestUser("Alice", 25);

        producer.sendMessage(user);

        ArgumentCaptor<ProducerRecord<String, byte[]>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockKafkaProducer).send(captor.capture());

        ProducerRecord<String, byte[]> record = captor.getValue();
        String json = new String(record.value(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"name\":\"Alice\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    public void testSendMessage_PropertiesMappedToHeadersAndKeyMapped() throws Exception {
        TestUser user = new TestUser("Bob", 30);

        producer.sendMessage(user, msg -> {
            msg.key("my-key")
               .eventTime(1600000000000L)
               .property("header-k1", "header-v1");
        });

        ArgumentCaptor<ProducerRecord<String, byte[]>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockKafkaProducer).send(captor.capture());

        ProducerRecord<String, byte[]> record = captor.getValue();
        assertEquals("my-key", record.key());
        assertEquals(1600000000000L, record.timestamp());

        Header header = record.headers().lastHeader("header-k1");
        assertNotNull(header);
        assertEquals("header-v1", new String(header.value(), StandardCharsets.UTF_8));
    }

    @Test
    public void testSendMessageAsync_Succeeds() throws Exception {
        TestUser user = new TestUser("Charlie", 35);

        CompletableFuture<IProducer.SendResult<TestUser>> future = producer.sendMessageAsync(user, msg -> {
            msg.key("my-async-key");
        });

        assertNotNull(future);
        IProducer.SendResult<TestUser> result = future.get();
        assertTrue(result.isSuccess());
        assertEquals("123", result.getMessage().getMessageId());
        assertEquals("my-async-key", result.getMessage().getKey());
    }

    @Test
    public void testClientClose_ClosesActiveProducers() throws Exception {
        assertFalse(client.isClosed());

        client.close();

        assertTrue(client.isClosed());
        verify(mockKafkaProducer).close();
    }
}
