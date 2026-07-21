package org.carl.infra.mq.pulsar.builder;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.client.MQClientProvider;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;

/** Pulsar implementation discovered by the common MQ client factory. */
public final class PulsarMQClientProvider implements MQClientProvider {

    @Override
    public String name() {
        return "pulsar";
    }

    @Override
    public MQClient create(MQConfig config) throws MQClientException {
        return MQClientBuilder.createPulsarClient(config);
    }
}
