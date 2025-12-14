package org.carl.infrastructure.pulsar.factory;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.ConsumerException;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.consumer.IConsumer;
import org.carl.infrastructure.mq.pulsar.builder.MQClientBuilder;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;
import org.carl.infrastructure.pulsar.model.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class ConsumerTest {
    MQClient client;

    @BeforeEach
    public void setUp() throws MQClientException {
        //        MsgConfig pulsarMsgConfig = new PulsarMsgConfig("pulsar://172.16.252.194:16650");
        MQConfig pulsarMsgConfig = new PulsarConfig("pulsar://180.184.66.147:6650");
        client = MQClientBuilder.createClient(pulsarMsgConfig);
    }

    //    @AfterEach
    //    public void tearDown() throws PulsarClientException {
    //        client.close();
    //    }

    @Test
    void consumerTest() throws InterruptedException, ConsumerException {
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
}
