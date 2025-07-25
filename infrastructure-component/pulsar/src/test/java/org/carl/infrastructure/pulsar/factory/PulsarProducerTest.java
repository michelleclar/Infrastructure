package org.carl.infrastructure.pulsar.factory;

import io.quarkus.test.junit.QuarkusTest;

import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.pulsar.common.ex.ProducerException;
import org.carl.infrastructure.pulsar.config.GlobalShare;
import org.carl.infrastructure.pulsar.core.PulsarProducer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

@QuarkusTest
class PulsarProducerTest {
    public static final String TOPIC = "testTopic";

    @Test
    void testPulsarProducer() throws PulsarClientException {
        PulsarProducer<String> pulsarProducer = buildPulsarProducer(TOPIC);
        System.out.println(pulsarProducer);
    }

    @Test
    void testSendMessage() throws ProducerException, PulsarClientException {
        PulsarProducer<String> pulsarProducer = buildPulsarProducer(TOPIC);
        pulsarProducer.sendMessage("hello");
        pulsarProducer.sendMessage("product");
        pulsarProducer.sendMessage("msg1");
        pulsarProducer.sendMessage("msg2");
        pulsarProducer.sendMessage("msg3");
    }

    @Test
    void testSendMessageAsync()
            throws ProducerException,
                    PulsarClientException,
                    InterruptedException,
                    ExecutionException {
        PulsarProducer<String> pulsarProducer = buildPulsarProducer(TOPIC);
        pulsarProducer.sendMessageAsync("hello").get();
    }

    private PulsarProducer<String> buildPulsarProducer(String topic) throws ProducerException {
        return new PulsarProducer<>(
                GlobalShare.getInstance().client(),
                GlobalShare.getInstance().producerConfig(),
                topic,
                String.class);
    }
}
