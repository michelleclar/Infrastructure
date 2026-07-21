package org.carl.infra.mq.kafka;

import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.client.MQClientProvider;
import org.carl.infra.mq.common.ex.MQClientException;
import org.carl.infra.mq.config.MQConfig;
import org.carl.infra.mq.kafka.builder.KafkaMQClient;

/** Kafka implementation discovered by the common MQ client factory. */
public final class KafkaMQClientProvider implements MQClientProvider {

    @Override
    public String name() {
        return "kafka";
    }

    @Override
    public MQClient create(MQConfig config) throws MQClientException {
        return new KafkaMQClient(config);
    }
}
