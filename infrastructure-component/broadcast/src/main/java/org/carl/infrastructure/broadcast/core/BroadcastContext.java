package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;

public class BroadcastContext {
    EventBus eventBus;

    public BroadcastContext(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public EventBus getEventBus() {
        return this.eventBus;
    }
}
