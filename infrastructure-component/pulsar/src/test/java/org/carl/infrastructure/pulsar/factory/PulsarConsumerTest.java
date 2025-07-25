package org.carl.infrastructure.pulsar.factory;

import static org.carl.infrastructure.pulsar.factory.PulsarProducerTest.TOPIC;

import io.quarkus.test.junit.QuarkusTest;

import org.carl.infrastructure.pulsar.builder.MessageBuilder;
import org.carl.infrastructure.pulsar.common.ex.ConsumerException;
import org.carl.infrastructure.pulsar.config.GlobalShare;
import org.carl.infrastructure.pulsar.core.IConsumer;
import org.carl.infrastructure.pulsar.core.PulsarConsumer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@QuarkusTest
class PulsarConsumerTest {

    @Test
    void testPulsarConsumer() {
        IConsumer<String> pulsarConsumer = buildPulsarConsumer(TOPIC);
        System.out.println(pulsarConsumer);
    }

    private IConsumer<String> buildPulsarConsumer(String topic) {
        return new PulsarConsumer<>(
                        GlobalShare.getInstance().client(),
                        GlobalShare.getInstance().consumerConfig(),
                        topic,
                        String.class)
                .subscribeName("sub-test")
                .setMessageListener(
                        (consumer, message) -> {
                            System.out.println("==========" + message.getValue());
                        });
    }

    @Test
    void subscribeName() {}

    @Test
    void testSubscribe() throws ConsumerException, InterruptedException {
        IConsumer<String> pulsarConsumer = buildPulsarConsumer(TOPIC);
        IConsumer<String> subscribe = pulsarConsumer.subscribe();
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        subscribe.close();
    }

    @Test
    void testReceive() throws ConsumerException, InterruptedException {
        IConsumer<String> pulsarConsumer = buildPulsarConsumer(TOPIC);
        MessageBuilder.Message<String> receive = pulsarConsumer.receive();
        System.out.println(receive.getValue());
    }

    @Test
    void receiveAsync() throws ConsumerException, ExecutionException, InterruptedException {
        IConsumer<String> pulsarConsumer = buildPulsarConsumer(TOPIC);
        pulsarConsumer
                .receiveAsync()
                .whenComplete(
                        (msg, ex) -> {
                            System.out.println(msg.getValue());
                        });
    }

    @Test
    void setMessageListener() {}

    @Test
    void batchReceive() {}

    @Test
    void testBatchReceive() {}

    @Test
    void batchReceiveAsync() {}

    @Test
    void acknowledge() {}

    @Test
    void acknowledgeAsync() {}

    @Test
    void testAcknowledge() {}

    @Test
    void testAcknowledgeAsync() {}

    @Test
    void acknowledgeCumulative() {}

    @Test
    void acknowledgeCumulativeAsync() {}

    @Test
    void negativeAcknowledge() {}

    @Test
    void testNegativeAcknowledge() {}

    @Test
    void pause() {}

    @Test
    void resume() {}

    @Test
    void isPaused() {}

    @Test
    void seek() {}

    @Test
    void testSeek() {}

    @Test
    void seekToBeginning() {}

    @Test
    void seekToEnd() {}

    @Test
    void getStats() {}

    @Test
    void isConnected() {}

    @Test
    void getTopic() {}

    @Test
    void getSubscription() {}

    @Test
    void getConsumerName() {}

    @Test
    void getSubscriptionType() {}

    @Test
    void close() {}

    @Test
    void closeAsync() {}
}
