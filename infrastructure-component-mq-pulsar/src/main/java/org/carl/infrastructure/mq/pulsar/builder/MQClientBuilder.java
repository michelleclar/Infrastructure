package org.carl.infrastructure.mq.pulsar.builder;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;

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
            return new PulsarMQClient(pulsarClient, config.producer(), config.consumer());
        } catch (PulsarClientException e) {
            throw new MQClientException(e);
        }
    }
}
