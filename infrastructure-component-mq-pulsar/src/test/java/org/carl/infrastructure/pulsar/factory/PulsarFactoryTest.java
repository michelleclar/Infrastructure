package org.carl.infrastructure.pulsar.factory;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;
import org.carl.infrastructure.mq.pulsar.factory.MQClientFactory;
import org.junit.jupiter.api.Test;

class PulsarFactoryTest {

    @Test
    void createClient() throws MQClientException {
        MQConfig pulsarMsgConfig = new PulsarConfig("pulsar://172.16.252.194:16650");
        MQClient client = MQClientFactory.createClient(pulsarMsgConfig);
        client.close();
    }
}
