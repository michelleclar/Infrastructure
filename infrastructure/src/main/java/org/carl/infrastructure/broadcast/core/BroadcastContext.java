package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;

import org.apache.pulsar.client.api.PulsarClient;

public class BroadcastContext {
    PulsarClient pulsarClient;
    EventBus eventBus;

    public BroadcastContext(PulsarClient pulsarClient, EventBus eventBus) {
        this.eventBus = eventBus;
        this.pulsarClient = pulsarClient;
    }
}
