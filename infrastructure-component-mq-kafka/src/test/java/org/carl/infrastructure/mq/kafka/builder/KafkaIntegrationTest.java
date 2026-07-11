package org.carl.infrastructure.mq.kafka.builder;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.kafka.config.KafkaConfig;
import org.carl.infrastructure.mq.model.Message;
import org.carl.infrastructure.mq.producer.IProducer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class KafkaIntegrationTest {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    private static MQClient mqClient;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestUser testUser = (TestUser) o;
            return age == testUser.age && Objects.equals(name, testUser.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    @BeforeAll
    public static void setUp() throws Exception {
        String bootstrapServers = KAFKA.getBootstrapServers();
        KafkaConfig config = new KafkaConfig(bootstrapServers);
        mqClient = MQClientBuilder.createClient(config);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        if (mqClient != null) {
            mqClient.close();
        }
    }

    @Test
    public void testRawProduceConsume() throws Exception {
        String topic = "raw-integration-test-topic";

        // 1. Create raw producer
        IProducer<byte[]> producer = mqClient.newProducer()
                .create(topic);

        // 2. Create raw consumer
        IConsumer<byte[]> consumer = mqClient.newConsumer()
                .subscriptionName("raw-integration-sub")
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscribe(topic);

        try {
            // 3. Send raw message
            byte[] payload = "Hello raw Kafka Integration Test!".getBytes(StandardCharsets.UTF_8);
            IProducer.SendResult<byte[]> result = producer.sendMessage(payload);
            assertTrue(result.isSuccess());

            // 4. Receive message and verify
            Message<byte[]> message = consumer.receive(15, TimeUnit.SECONDS);
            assertNotNull(message, "Should have received a message");
            assertArrayEquals(payload, message.getValue());
        } finally {
            // 5. Gracefully close producer and consumer
            producer.close();
            consumer.close();
        }
    }

    @Test
    public void testTypedProduceConsumeDirectReceive() throws Exception {
        String topic = "typed-direct-integration-test-topic";

        // 1. Create typed producer
        IProducer<TestUser> producer = mqClient.newProducer(TestUser.class)
                .create(topic);

        // 2. Create typed consumer
        IConsumer<TestUser> consumer = mqClient.newConsumer(TestUser.class)
                .subscriptionName("typed-direct-integration-sub")
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscribe(topic);

        try {
            // 3. Send typed message
            TestUser sentUser = new TestUser("John Doe", 30);
            IProducer.SendResult<TestUser> result = producer.sendMessage(sentUser);
            assertTrue(result.isSuccess());

            // 4. Receive message and verify JSON serialization/deserialization
            Message<TestUser> message = consumer.receive(15, TimeUnit.SECONDS);
            assertNotNull(message, "Should have received a message");
            assertEquals(sentUser, message.getValue());
        } finally {
            // 5. Gracefully close producer and consumer
            producer.close();
            consumer.close();
        }
    }

    @Test
    public void testTypedProduceConsumeWithListener() throws Exception {
        String topic = "typed-listener-integration-test-topic";

        // 1. Create typed producer
        IProducer<TestUser> producer = mqClient.newProducer(TestUser.class)
                .create(topic);

        BlockingQueue<Message<TestUser>> receivedMessages = new LinkedBlockingQueue<>();

        // 2. Create typed consumer with MessageListener
        IConsumer<TestUser> consumer = mqClient.newConsumer(TestUser.class)
                .subscriptionName("typed-listener-integration-sub")
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .messageListener((cons, msg) -> {
                    receivedMessages.offer(msg);
                })
                .subscribe(topic);

        try {
            // 3. Send typed message
            TestUser sentUser = new TestUser("Jane Smith", 28);
            IProducer.SendResult<TestUser> result = producer.sendMessage(sentUser);
            assertTrue(result.isSuccess());

            // 4. Receive message via listener and verify
            Message<TestUser> message = receivedMessages.poll(15, TimeUnit.SECONDS);
            assertNotNull(message, "Should have received a message via listener");
            assertEquals(sentUser, message.getValue());
        } finally {
            // 5. Gracefully close producer and consumer
            producer.close();
            consumer.close();
        }
    }
}
