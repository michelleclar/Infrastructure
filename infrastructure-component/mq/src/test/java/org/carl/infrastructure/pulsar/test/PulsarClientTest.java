package org.carl.infrastructure.pulsar.test;

import org.apache.pulsar.client.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class PulsarClientTest {
    PulsarClient client;

    @BeforeEach
    public void init() throws PulsarClientException {
        client = PulsarClient.builder().serviceUrl("pulsar://localhost:6650").build();
    }

    @Test
    public void testProducer() throws PulsarClientException {
        try (Producer<byte[]> producer =
                client.newProducer().topic("persistent://public/default/topic-1").create()) {
            producer.newMessage()
                    .key("msgKey1")
                    .value("hello".getBytes(StandardCharsets.UTF_8))
                    .property("p1", "v1")
                    .property("p2", "v2")
                    .send();
        }
    }

    @Test
    public void testSyncConsumer() throws PulsarClientException {
        try (Consumer<byte[]> consumer =
                client.newConsumer()
                        .topic("persistent://public/default/topic-1")
                        .subscriptionName("sub-2")
                        .subscriptionType(SubscriptionType.Exclusive)
                        .subscribe()) {
            while (true) {
                // 等待一个消息
                Message<byte[]> msg = consumer.receive();
                try {
                    // 处理消息
                    System.out.println("Message received: " + new String(msg.getData()));
                    // 处理完成发送确认ACK, 通知Broker消息可以被删除
                    consumer.acknowledge(msg);
                } catch (Exception e) {
                    // 处理失败，发送否定确认(negative ack)，在稍后的时间消息会重新发给消费者进行重试
                    consumer.negativeAcknowledge(msg);
                }
            }
        }
    }

    @Test
    public void testAsyncConsumer() throws PulsarClientException, InterruptedException {
        ConsumerBuilder<byte[]> consumerBuilder =
                client.newConsumer()
                        .topic("persistent://public/default/topic-1")
                        .subscriptionName("sub-4")
                        .subscriptionType(SubscriptionType.Shared) // 订阅类型: 共享模式
                        .messageListener(
                                (c, msg) -> {
                                    try {
                                        System.out.println(
                                                c.getConsumerName()
                                                        + " received: "
                                                        + new String(msg.getData()));
                                        c.acknowledge(msg);
                                    } catch (Exception e) {
                                        c.negativeAcknowledge(msg);
                                    }
                                });
        for (int i = 0; i < 4; i++) {
            consumerBuilder.consumerName("testConsumerMessageListener-" + i).subscribe();
        }
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
    }

    @AfterEach
    public void close() throws PulsarClientException {
        client.close();
    }
}
