package org.carl.infrastructure.pulsar.factory;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infrastructure.pulsar.builder.IConsumer;
import org.carl.infrastructure.pulsar.builder.MessageBuilder;
import org.carl.infrastructure.pulsar.builder.PulsarConsumerBuilder;
import org.carl.infrastructure.pulsar.common.ex.ConsumerException;
import org.carl.infrastructure.pulsar.model.TestUser;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@QuarkusTest
class PulsarConsumerTest {
    @Inject PulsarClient client;

    @Test
    void testSubscribe() throws ConsumerException, InterruptedException {
        IConsumer<TestUser> user =
                PulsarConsumerBuilder.create(client, TestUser.class)
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
                        .topic("user")
                        .subscribe();

        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        user.close();
    }

    @Test
    void testReceive() throws ConsumerException {
        IConsumer<TestUser> user =
                PulsarConsumerBuilder.create(client, TestUser.class)
                        .subscriptionInitialPosition(IConsumer.SubscriptionInitialPosition.Earliest)
                        .subscriptionMode(IConsumer.SubscriptionMode.NonDurable)
                        .subscriptionName("test-receive")
                        .topic("user")
                        .subscribe();

        MessageBuilder.Message<TestUser> receive = user.receive();
        System.out.println(receive.getValue());
    }
}
