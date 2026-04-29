package org.carl.infrastructure.broadcast;

import io.vertx.mutiny.core.eventbus.MessageConsumer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BroadcastRegistry {
    public static BroadcastRegistry instance;

    static {
        instance = new BroadcastRegistry();
    }

    private BroadcastRegistry() {}

    private final Map<String, MessageConsumer<?>> consumerMap = new ConcurrentHashMap<>();

    public boolean has(String topic) {
        return consumerMap.containsKey(topic);
    }

    public void put(String topic, MessageConsumer<?> consumer) {
        consumerMap.put(topic, consumer);
    }

    public MessageConsumer<?> remove(String topic) {
        return consumerMap.remove(topic);
    }

    public Collection<MessageConsumer<?>> all() {
        return consumerMap.values();
    }

    public void clear() {
        consumerMap.clear();
    }
}
