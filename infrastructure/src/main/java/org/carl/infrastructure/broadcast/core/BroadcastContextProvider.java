package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.apache.pulsar.client.api.PulsarClient;

public class BroadcastContextProvider implements Provider<BroadcastContext> {
    @Inject PulsarClient pulsarClient;
    @Inject EventBus eventBus;

    @Produces
    @ApplicationScoped
    @Override
    public BroadcastContext get() {
        return new BroadcastContext(pulsarClient, eventBus);
    }
}
