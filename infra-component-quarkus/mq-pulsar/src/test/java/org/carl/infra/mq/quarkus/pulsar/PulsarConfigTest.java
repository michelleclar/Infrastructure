package org.carl.infra.mq.quarkus.pulsar;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.config.MsgArgsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class PulsarConfigTest {
    @Inject MsgArgsConfig config;
    @Inject MQClient client;

    @Test
    void shouldProducePulsarClientFromMappedConfiguration() {
        assertEquals("pulsar://localhost:6650", config.client().serviceUrl());
        assertEquals(
                "org.carl.infra.mq.pulsar.builder.PulsarProducerBuilder",
                client.newProducer().getClass().getName());
    }
}
