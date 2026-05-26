package org.carl.infrastructure.broadcast.ability;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;

import org.carl.infrastructure.broadcast.IBroadcastOperations;

import java.util.function.Consumer;

public interface IBroadcastAbility {
    IBroadcastOperations getBroadcastOperations();

    default <T> EventBus publish(String topic, T message) {
        return getBroadcastOperations().publish(topic, message);
    }

    default <T> Uni<Object> request(String topic, T message) {
        return getBroadcastOperations().request(topic, message);
    }

    default <T> Uni<Void> subscribe(String topic, Class<T> type, Consumer<T> handler) {
        return getBroadcastOperations().subscribe(topic, type, handler);
    }

    default Uni<Void> unsubscribe(String topic) {
        return getBroadcastOperations().unsubscribe(topic);
    }
}
