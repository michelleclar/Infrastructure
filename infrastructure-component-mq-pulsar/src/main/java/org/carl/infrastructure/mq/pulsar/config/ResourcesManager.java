package org.carl.infrastructure.mq.pulsar.config;

import org.apache.commons.lang3.StringUtils;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourcesManager {
    protected static Map<String, MQClient> MQClients = new ConcurrentHashMap<>();

    public static void add(String name, MQClient MQClient) {
        if (StringUtils.isBlank(name)) {
            throw new RuntimeException("name is empty");
        }
        MQClients.put(name, MQClient);
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
        MQClients.forEach(
                (k, v) -> {
                    if (!v.isClosed()) {
                        try {
                            v.close();
                        } catch (MQClientException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
        MQClients.clear();
    }
}
