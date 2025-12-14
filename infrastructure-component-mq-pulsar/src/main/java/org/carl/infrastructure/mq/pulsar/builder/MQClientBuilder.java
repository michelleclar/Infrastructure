package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.pulsar.config.ResourcesManager;

import java.util.UUID;

public class MQClientBuilder {

    private static final ILogger logger = LoggerFactory.getLogger(MQClientBuilder.class);

    public static MQClient createClient(MQConfig config) throws MQClientException {

        logger.debug(
                "Creating Pulsar client with args: \n client: [{}] transaction: [{}] monitor: [{}]",
                config.client(),
                config.transaction(),
                config.monitoring());
        ClientBuilder build;
        try {
            build =
                    PulsarClientFactory.getInstance()
                            .processConnect(config.client())
                            .process(config.transaction())
                            .process(config.monitoring())
                            .build();
        } catch (PulsarClientException.UnsupportedAuthenticationException e) {
            throw new MQClientException(e);
        }
        try {
            PulsarClient pulsarClient = build.build();
            PulsarMQClient pulsarMQClient =
                    new PulsarMQClient(pulsarClient, config.producer(), config.consumer());
            ResourcesManager.add(
                    config.name().isEmpty() ? UUID.randomUUID().toString() : config.name().get(),
                    pulsarMQClient);
            return pulsarMQClient;
        } catch (PulsarClientException e) {
            throw new MQClientException(e);
        }
    }
}
