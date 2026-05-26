package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class BroadcastContextProvider {
    @Inject EventBus eventBus;

    @Produces
    @ApplicationScoped
    public BroadcastContext get() {
        return new BroadcastContext(eventBus);
    }
}
