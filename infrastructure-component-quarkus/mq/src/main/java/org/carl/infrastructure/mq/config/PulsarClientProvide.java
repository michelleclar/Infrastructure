package org.carl.infrastructure.mq.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.pulsar.builder.MQClientBuilder;

public class PulsarClientProvide {

    @Inject MsgArgsConfig msgArgsConfig;

    @Produces
    @ApplicationScoped
    public MQClient get() throws MQClientException {
        return MQClientBuilder.createClient(msgArgsConfig);
    }
}
