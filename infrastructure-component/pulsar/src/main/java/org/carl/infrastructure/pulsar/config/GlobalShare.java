package org.carl.infrastructure.pulsar.config;

import org.apache.pulsar.client.api.PulsarClient;

public class GlobalShare {
    private static final GlobalShare INSTANCE;

    private GlobalShare() {}

    static {
        INSTANCE = new GlobalShare();
    }

    public static GlobalShare getInstance() {
        return INSTANCE;
    }

    protected PulsarClient pulsarClient;
    protected MsgArgsConfig msgArgsConfig;

    public PulsarClient client() {
        return pulsarClient;
    }

    public MsgArgsConfig pulsarArgsConfig() {
        return msgArgsConfig;
    }

    public MsgArgsConfig.ClientConfig clientConfig() {
        return pulsarArgsConfig().client();
    }

    public MsgArgsConfig.ProducerConfig producerConfig() {
        return pulsarArgsConfig().producer();
    }

    public MsgArgsConfig.ConsumerConfig consumerConfig() {
        return pulsarArgsConfig().consumer();
    }

    public MsgArgsConfig.TransactionConfig transactionConfig() {
        return pulsarArgsConfig().transaction();
    }
}
