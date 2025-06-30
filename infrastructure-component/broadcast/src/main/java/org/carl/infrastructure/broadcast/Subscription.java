package org.carl.infrastructure.broadcast;

import io.smallrye.mutiny.Uni;

public interface Subscription {
    Uni<Void> unsubscribe();
}
