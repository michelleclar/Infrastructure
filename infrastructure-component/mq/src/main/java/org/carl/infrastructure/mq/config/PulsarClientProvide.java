package org.carl.infrastructure.mq.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.pulsar.config.PulsarConfig;
import org.carl.infrastructure.mq.pulsar.factory.MQClientFactory;

public class PulsarClientProvide {

    @Inject MsgArgsConfig msgArgsConfig;

    @Produces
    @ApplicationScoped
    public MQClient get() throws MQClientException {
        // TODO MsgConfig
        PulsarConfig convert = MsgConfigConverter.convert(msgArgsConfig);
        return MQClientFactory.createClient(convert);
    }
}
