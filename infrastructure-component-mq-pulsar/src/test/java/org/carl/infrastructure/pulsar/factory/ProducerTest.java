package org.carl.infrastructure.pulsar.factory;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.producer.IProducer;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;
import org.carl.infrastructure.mq.pulsar.factory.MQClientFactory;
import org.carl.infrastructure.pulsar.model.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProducerTest {
    MQClient client;

    @BeforeEach
    public void setUp() throws MQClientException {
        //        MsgConfig pulsarMsgConfig = new PulsarMsgConfig("pulsar://172.16.252.194:16650");
        MQConfig pulsarMsgConfig = new PulsarConfig("pulsar://180.184.66.147:6650");

        client = MQClientFactory.createClient(pulsarMsgConfig);
    }

    @AfterEach
    public void tearDown() throws MQClientException {
        client.close();
    }

    @Test
    void producerTest() throws Exception {
        IProducer<TestUser> testUserIProducer = client.newProducer(TestUser.class).create("user");
        testUserIProducer.sendMessage(randomUser());
        System.out.println("Message sent successfully!");
        testUserIProducer.close();
    }

    private static TestUser randomUser() {
        return new TestUser(2L, "1", "2", 1);
    }
}
