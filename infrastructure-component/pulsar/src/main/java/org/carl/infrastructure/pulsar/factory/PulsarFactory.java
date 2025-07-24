package org.carl.infrastructure.pulsar.factory;

import org.apache.pulsar.client.api.*;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.carl.infrastructure.pulsar.config.PulsarClientFactory;
import org.jboss.logging.Logger;

/** Pulsar 工厂类 简化 PulsarClient 和 PulsarMessageManager 的创建 */
public class PulsarFactory {

    private static final Logger logger = Logger.getLogger(PulsarFactory.class);

    /**
     * @param msgArgsConfig {@link MsgArgsConfig}
     * @return {@link PulsarClient}
     */
    public static PulsarClient createClient(MsgArgsConfig msgArgsConfig)
            throws PulsarClientException {

        logger.debugf("Creating Pulsar client with args: {%s}", msgArgsConfig);
        ClientBuilder build =
                PulsarClientFactory.getInstance()
                        .processConnect(msgArgsConfig.client())
                        .process(msgArgsConfig.transaction())
                        .process(msgArgsConfig.monitoring())
                        .build();
        return build.build();
    }

    public static <T> Consumer<T> createConsumer(MsgArgsConfig.ConsumerConfig consumerConfig) {
        return null;
    }

    public static Consumer<byte[]> createConsumer(
            String topic, MessageListener<byte[]> messageListener, String subscriptionName)
            throws PulsarClientException {
        //        return
        // PulsarConsumerBuilder.create(topic).build(subscriptionName,messageListener);
        return null;
    }

    public static Producer<byte[]> createProducer(String topic) throws PulsarClientException {
        //        return ProducerBuilder.create(topic).createProducer();
        return null;
    }

    public static <T> Producer<T> createProducer(MsgArgsConfig.ProducerConfig producerConfig) {
        return null;
    }
}
