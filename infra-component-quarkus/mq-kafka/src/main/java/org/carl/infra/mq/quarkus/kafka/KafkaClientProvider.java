package org.carl.infra.mq.quarkus.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MsgArgsConfig;
import org.carl.infra.mq.kafka.builder.MQClientBuilder;

/** Produces the application-scoped Kafka implementation of {@link MQClient}. */
public class KafkaClientProvider {

    @Inject MsgArgsConfig config;

    @Produces
    @ApplicationScoped
    public MQClient get() throws MQClientException {
        return MQClientBuilder.createClient(config);
    }
}
