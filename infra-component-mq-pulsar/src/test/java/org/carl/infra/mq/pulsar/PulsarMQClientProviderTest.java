package org.carl.infra.mq.pulsar;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.client.MQClientFactory;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PulsarMQClientProviderTest {

    @Test
    void shouldCreatePulsarClientThroughCommonFactory() throws Exception {
        MQClient client = MQClientFactory.create(new PulsarConfig("pulsar://localhost:6650"));

        try {
            assertEquals("org.carl.infra.mq.pulsar.builder.PulsarMQClient", client.getClass().getName());
        } finally {
            client.close();
        }
    }
}
