package org.carl.infra.pulsar.factory;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.pulsar.builder.MQClientBuilder;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.junit.jupiter.api.Test;

class PulsarFactoryTest {

    @Test
    void createClient() throws MQClientException {
        MQConfig pulsarMsgConfig = new PulsarConfig("pulsar://172.16.252.194:16650");
        MQClient client = MQClientBuilder.createClient(pulsarMsgConfig);
        client.close();
    }
}
