package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;
import org.apache.pulsar.client.api.ClientBuilder;

public class BroadcastContext {
    ClientBuilder pulsarClientBuilder;
    EventBus eventBus;

    public BroadcastContext(ClientBuilder builder, EventBus eventBus) {
        this.eventBus = eventBus;
        this.pulsarClientBuilder = builder;
    }
}
