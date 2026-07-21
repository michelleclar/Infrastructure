package org.carl.infra.mq.pulsar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.consumer.IConsumer;
import org.carl.infra.mq.consumer.SubscriptionInitialPosition;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.model.Message;
import org.carl.infra.mq.producer.IProducer;
import org.carl.infra.mq.pulsar.builder.MQClientBuilder;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@EnabledIfEnvironmentVariable(named = "CLOUDEMQ_SERVICE_URL", matches = ".+")
class CloudEmqIntegrationTest {

    @Test
    void shouldSendAndReceiveThroughPublicMqApi() throws Exception {
        String serviceUrl = System.getenv("CLOUDEMQ_SERVICE_URL");
        String runId = UUID.randomUUID().toString();
        String topic = "infra-cloudemq-smoke-" + runId;
        String subscription = "infra-cloudemq-smoke-sub-" + runId;
        byte[] payload = ("cloudemq-smoke-" + runId).getBytes(StandardCharsets.UTF_8);

        MQClient client = MQClientBuilder.createClient(new PulsarConfig(serviceUrl));
        IConsumer<byte[]> consumer = null;
        IProducer<byte[]> producer = null;
        try {
            consumer =
                    client.newConsumer()
                            .subscriptionName(subscription)
                            .subscriptionType(SubscriptionTypes.LOAD_BALANCED)
                            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                            .subscribe(topic);
            producer = client.newProducer().create(topic);

            IProducer.SendResult<byte[]> sendResult = producer.sendMessage(payload);
            assertTrue(sendResult.isSuccess(), sendResult.getErrorMessage());

            Message<byte[]> received = consumer.receive(20, TimeUnit.SECONDS);
            assertNotNull(received, "CloudEMQ did not deliver the test message within 20 seconds");
            assertArrayEquals(payload, received.getValue());
            consumer.acknowledge(received);
        } finally {
            if (producer != null) {
                producer.closeAsync().get(10, TimeUnit.SECONDS);
            }
            if (consumer != null) {
                consumer.close();
            }
            client.close();
        }
    }
}
