package org.carl.infrastructure.broadcast;

import org.carl.infrastructure.broadcast.core.BroadcastContext;

/** now use eventbus driven message queue TODO: need design topic rule */
public interface IBroadcastProvider {
    BroadcastContext getBroadcastContext();

    BroadcastRegistry registry();

    void setBroadcastContext(BroadcastContext broadcastContext);
}
