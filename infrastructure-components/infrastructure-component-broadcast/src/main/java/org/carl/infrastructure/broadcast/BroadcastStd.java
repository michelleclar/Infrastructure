package org.carl.infrastructure.broadcast;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.carl.infrastructure.broadcast.core.BroadcastContext;

@ApplicationScoped
public class BroadcastStd implements IBroadcastOperations{
    @Inject BroadcastContext broadcastContext;

    @Override
    public BroadcastContext getBroadcastContext() {
        return broadcastContext;
    }

    @Override
    public void setBroadcastContext(BroadcastContext broadcastContext) {
        this.broadcastContext = broadcastContext;
    }

    @Override
    public BroadcastRegistry registry() {
        return BroadcastRegistry.instance;
    }
}
