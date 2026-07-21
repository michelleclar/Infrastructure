package org.carl.infra.mq.quarkus.kafka;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.config.MsgArgsConfig;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.kafka.builder.KafkaProducerBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@QuarkusTest
class KafkaConfigTest {
    @Inject MsgArgsConfig config;
    @Inject MQClient client;

    @Test
    void shouldProduceKafkaClientFromMappedConfiguration() {
        assertEquals("localhost:9092", config.client().serviceUrl());
        assertSame(
                SubscriptionInitialPosition.Latest,
                config.consumer().subscriptionInitialPosition());
        assertSame(SubscriptionTypes.LOAD_BALANCED, config.consumer().subscriptionType());
        assertInstanceOf(KafkaProducerBuilder.class, client.newProducer());
    }
}
