package org.carl.infra.mq.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.kafka.builder.MQClientBuilder;

public class KafkaClientProvider {

    @Inject MsgArgsConfig msgArgsConfig;

    @Produces
    @ApplicationScoped
    public MQClient get() throws MQClientException {
        return MQClientBuilder.createClient(msgArgsConfig);
    }
}
