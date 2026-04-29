package org.carl.infrastructure.pulsar.factory;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.consumer.SubscriptionInitialPosition;
import org.carl.infrastructure.mq.consumer.SubscriptionMode;
import org.carl.infrastructure.mq.model.Message;
import org.carl.infrastructure.pulsar.model.TestUser;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@QuarkusTest
class PulsarConsumerTest {
    @Inject MQClient client;

    @Test
    void testSubscribe() throws ConsumerException, InterruptedException {
        IConsumer<TestUser> user =
                client.newConsumer(TestUser.class)
                        .subscriptionName("test-receive")
                        .messageListener(
                                (consumer, msg) -> {
                                    System.out.println("---------" + msg.getValue() + "----------");
                                    try {
                                        consumer.acknowledge(msg);
                                    } catch (ConsumerException e) {
                                        consumer.negativeAcknowledge(msg);
                                        throw new RuntimeException(e);
                                    }
                                })
                        .subscribe("user");

        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        user.close();
    }

    @Test
    void testReceive() throws ConsumerException {
        IConsumer<TestUser> user =
                client.newConsumer(TestUser.class)
                        .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                        .subscriptionMode(SubscriptionMode.NonDurable)
                        .subscriptionName("test-receive")
                        .subscribe("user");

        Message<TestUser> receive = user.receive();
        System.out.println(receive.getValue());
    }
}
