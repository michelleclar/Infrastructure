package org.carl.infra.mq.quarkus.pulsar;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MsgArgsConfig;
import org.carl.infra.mq.pulsar.builder.MQClientBuilder;

/** Produces the application-scoped Pulsar implementation of {@link MQClient}. */
public class PulsarClientProvider {

    @Inject MsgArgsConfig config;

    @Produces
    @ApplicationScoped
    public MQClient get() throws MQClientException {
        return MQClientBuilder.createClient(config);
    }
}
