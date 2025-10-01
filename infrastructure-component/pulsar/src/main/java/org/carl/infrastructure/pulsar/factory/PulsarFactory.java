package org.carl.infrastructure.pulsar.factory;

import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.pulsar.common.ex.ClientException;
import org.carl.infrastructure.pulsar.config.MsgArgsConfig;
import org.carl.infrastructure.pulsar.config.PulsarClientFactory;
import org.jboss.logging.Logger;

/** Pulsar 工厂类 简化 PulsarClient 和 PulsarMessageManager 的创建 */
public class PulsarFactory {

    private static final Logger logger = Logger.getLogger(PulsarFactory.class);

    public static PulsarClient createClient(
            MsgArgsConfig.ClientConfig clientConfig,
            MsgArgsConfig.TransactionConfig transactionConfig,
            MsgArgsConfig.MonitoringConfig monitoringConfig)
            throws ClientException {

        logger.debugf(
                "Creating Pulsar client with args: \n client: [%s] transaction: [%s] monitor: [%s]",
                clientConfig, transactionConfig, monitoringConfig);
        ClientBuilder build;
        try {
            build =
                    PulsarClientFactory.getInstance()
                            .processConnect(clientConfig)
                            .process(transactionConfig)
                            .process(monitoringConfig)
                            .build();
        } catch (PulsarClientException.UnsupportedAuthenticationException e) {
            throw new ClientException(e);
        }
        try {
            return build.build();
        } catch (PulsarClientException e) {
            throw new ClientException(e);
        }
    }

    //    public static IConsumer<byte[]> createConsumer(String topic) {
    //        return new PulsarConsumer<>(
    //                GlobalShare.getInstance().client(),
    //                GlobalShare.getInstance().consumerConfig(),
    //                topic,
    //                byte[].class);
    //    }
    //
    //    public static <T> IConsumer<T> createConsumer(Class<T> clazz, String topic) {
    //        return new PulsarConsumer<>(
    //                GlobalShare.getInstance().client(),
    //                GlobalShare.getInstance().consumerConfig(),
    //                topic,
    //                clazz);
    //    }

    //    public static <T> IConsumer<T> createConsumer(
    //            PulsarClient client, Class<T> clazz, String topic) {
    //        return new PulsarConsumer<>(
    //                client, GlobalShare.getInstance().consumerConfig(), topic, clazz);
    //    }

    //    public static <T> IConsumer<T> createConsumer(
    //            PulsarClient client,
    //            MsgArgsConfig.ConsumerConfig consumerConfig,
    //            Class<T> clazz,
    //            String topic) {
    //        return new PulsarConsumer<>(client, consumerConfig, topic, clazz);
    //    }
    //
    //    public static <T> IProducer<T> createProducer(Class<T> clazz, String topic)
    //            throws ProducerException {
    //        return new PulsarProducer<>(
    //                GlobalShare.getInstance().client(),
    //                GlobalShare.getInstance().producerConfig(),
    //                topic,
    //                clazz);
    //    }

    //    public static <T> IProducer<T> createProducer(PulsarClient client, Class<T> clazz, String
    // topic)
    //            throws ProducerException {
    //        return new PulsarProducer<>(
    //                client, GlobalShare.getInstance().producerConfig(), topic, clazz);
    //    }
    //
    //    public static <T> IProducer<T> createProducer(
    //            PulsarClient client,
    //            MsgArgsConfig.ProducerConfig producerConfig,
    //            Class<T> clazz,
    //            String topic)
    //            throws ProducerException {
    //        return new PulsarProducer<>(client, producerConfig, topic, clazz);
    //    }
    //
    //    public static IProducer<byte[]> createProducer(String topic) throws ProducerException {
    //        return new PulsarProducer<>(
    //                GlobalShare.getInstance().client(),
    //                GlobalShare.getInstance().producerConfig(),
    //                topic,
    //                byte[].class);
    //    }
}
