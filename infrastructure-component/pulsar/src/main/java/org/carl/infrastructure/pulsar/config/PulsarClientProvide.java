package org.carl.infrastructure.pulsar.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.apache.pulsar.client.api.PulsarClient;

public class PulsarClientProvide {
    @Produces
    @ApplicationScoped
    public PulsarClient get() {
        return GlobalShare.getInstance().pulsarClient;
    }
}
