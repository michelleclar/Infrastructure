package org.carl.infrastructure.kafka.test;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.config.MsgArgsConfig;
import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionType;
import org.carl.infrastructure.mq.kafka.builder.KafkaProducerBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
class KafkaConfigTest {
    @Inject MsgArgsConfig msgArgsConfig;
    @Inject MQClient mqClient;

    @Test
    void shouldProduceKafkaClientFromMappedConfiguration() {
        assertEquals("localhost:9092", msgArgsConfig.client().serviceUrl());
        assertSame(
                SubscriptionInitialPosition.Latest,
                msgArgsConfig.consumer().subscriptionInitialPosition());
        assertSame(SubscriptionType.EXCLUSIVE, msgArgsConfig.consumer().subscriptionType());
        assertInstanceOf(KafkaProducerBuilder.class, mqClient.newProducer());
    }
}
