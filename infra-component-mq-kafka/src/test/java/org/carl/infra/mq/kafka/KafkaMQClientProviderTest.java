package org.carl.infra.mq.kafka;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.client.MQClientFactory;
import org.carl.infra.mq.kafka.builder.KafkaMQClient;
import org.carl.infra.mq.kafka.config.KafkaConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class KafkaMQClientProviderTest {

    @Test
    void shouldCreateKafkaClientThroughCommonFactory() throws Exception {
        MQClient client = MQClientFactory.create(new KafkaConfig("localhost:9092"));

        try {
            assertInstanceOf(KafkaMQClient.class, client);
        } finally {
            client.close();
        }
    }
}
