package org.carl.infrastructure.mq.kafka.builder;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;
import org.carl.infrastructure.mq.config.MQConfig;
import org.carl.infrastructure.mq.kafka.config.ResourcesManager;

import java.util.UUID;

public class MQClientBuilder {

    private static final ILogger logger = LoggerFactory.getLogger(MQClientBuilder.class);

    public static MQClient createClient(MQConfig config) throws MQClientException {
        logger.debug(
                "Creating Kafka client with args: \n client: [{}] transaction: [{}] monitor: [{}]",
                config.client(),
                config.transaction(),
                config.monitoring());

        KafkaMQClient kafkaMQClient = new KafkaMQClient(config);
        ResourcesManager.add(
                config.name().isEmpty() ? UUID.randomUUID().toString() : config.name().get(),
                kafkaMQClient);
        return kafkaMQClient;
    }
}
