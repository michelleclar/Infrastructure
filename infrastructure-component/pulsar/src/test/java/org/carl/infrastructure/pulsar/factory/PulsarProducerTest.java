package org.carl.infrastructure.pulsar.factory;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.apache.commons.lang3.RandomUtils;
import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infrastructure.pulsar.builder.IProducer;
import org.carl.infrastructure.pulsar.builder.PulsarProducerBuilder;
import org.carl.infrastructure.pulsar.common.ex.ProducerException;
import org.carl.infrastructure.pulsar.model.TestUser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@QuarkusTest
class PulsarProducerTest {
    public static final String TOPIC = "testTopic";

    @Inject PulsarClient client;

    @Test
    void testSendMessage() throws ProducerException {
        var user = buildPulsarProducer(TestUser.class, "user");
        user.sendMessage(randomUser());
    }

    @Test
    void testSendMessageAsync() throws ProducerException, InterruptedException, ExecutionException {
        var pulsarProducer = buildPulsarProducer(TOPIC);
        pulsarProducer.sendMessageAsync("hello").get();
    }

    private IProducer<String> buildPulsarProducer(String topic) throws ProducerException {
        return buildPulsarProducer(String.class, topic);
    }

    private <T> IProducer<T> buildPulsarProducer(Class<T> clazz, String topic)
            throws ProducerException {
        PulsarProducerBuilder<T> tPulsarProducerBuilder =
                PulsarProducerBuilder.create(client, clazz);
        return tPulsarProducerBuilder.topic(topic).create();
    }

    private static TestUser randomUser() {
        RandomUtils insecure = RandomUtils.insecure();
        return new TestUser(
                insecure.randomLong(),
                Arrays.toString(insecure.randomBytes(4)),
                Arrays.toString(insecure.randomBytes(4)),
                insecure.randomInt());
    }
}
