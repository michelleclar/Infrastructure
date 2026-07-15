package org.carl.infra.kafka.test;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.config.MsgArgsConfig;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.kafka.builder.KafkaProducerBuilder;
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
