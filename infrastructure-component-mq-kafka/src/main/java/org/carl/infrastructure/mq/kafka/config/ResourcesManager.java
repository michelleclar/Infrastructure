package org.carl.infrastructure.mq.kafka.config;

import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcesManager {
    protected static Map<String, MQClient> MQClients = new ConcurrentHashMap<>();

    public static void add(String name, MQClient MQClient) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("name is empty");
        }
        MQClient previous = MQClients.putIfAbsent(name, MQClient);
        if (previous != null) {
            throw new IllegalStateException("MQClient already exists: " + name);
        }
    }

    public static MQClient get(String id) {
        return MQClients.get(id);
    }

    public static void remove(String id) throws MQClientException {
        MQClient remove = MQClients.remove(id);
        if (remove != null && !remove.isClosed()) {
            remove.close();
        }
    }

    public static void closeAll() {
        RuntimeException failure = null;
        for (MQClient client : MQClients.values()) {
            if (!client.isClosed()) {
                try {
                    client.close();
                } catch (MQClientException e) {
                    if (failure == null) {
                        failure = new RuntimeException("Failed to close one or more MQ clients", e);
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        MQClients.clear();
        if (failure != null) {
            throw failure;
        }
    }
}
