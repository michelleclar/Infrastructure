package org.carl.infra.mq.pulsar.builder;

import io.opentelemetry.api.OpenTelemetry;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
public class MQClientBuilder {

    private static final ILogger logger = LoggerFactory.getLogger(MQClientBuilder.class);

    public static MQClient createClient(MQConfig config, OpenTelemetry openTelemetry)
            throws MQClientException {

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
                            .process(config.monitoring(), openTelemetry)
                            .build();
        } catch (PulsarClientException.UnsupportedAuthenticationException e) {
            throw new MQClientException(e);
        }
        try {
            PulsarClient pulsarClient = build.build();
            PulsarAdmin pulsarAdmin = PulsarAdminFactory.create(config.client()).orElse(null);
            return new PulsarMQClient(
                    pulsarClient,
                    config.producer(),
                    config.consumer(),
                    pulsarAdmin,
                    PulsarTopicResolver.from(config));
        } catch (PulsarClientException e) {
            throw new MQClientException(e);
        }
    }

    public static MQClient createClient(MQConfig config) throws MQClientException {
        return createPulsarClient(config);
    }

    static MQClient createPulsarClient(MQConfig config) throws MQClientException {

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
            PulsarAdmin pulsarAdmin = PulsarAdminFactory.create(config.client()).orElse(null);
            return new PulsarMQClient(
                    pulsarClient,
                    config.producer(),
                    config.consumer(),
                    pulsarAdmin,
                    PulsarTopicResolver.from(config));
        } catch (PulsarClientException e) {
            throw new MQClientException(e);
        }
    }
}
