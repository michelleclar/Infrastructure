package org.carl.infra.mq.kafka.builder;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
public class MQClientBuilder {

    private static final ILogger logger = LoggerFactory.getLogger(MQClientBuilder.class);

    public static MQClient createClient(MQConfig config) throws MQClientException {
        logger.debug(
                "Creating Kafka client with args: \n client: [{}] transaction: [{}] monitor: [{}]",
                config.client(),
                config.transaction(),
                config.monitoring());

        return new KafkaMQClient(config);
    }
}
