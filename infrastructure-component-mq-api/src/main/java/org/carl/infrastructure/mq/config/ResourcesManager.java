package org.carl.infrastructure.mq.config;

import org.carl.infrastructure.mq.client.MQClient;

import java.util.ArrayList;
import java.util.List;

public class ResourcesManager {
    protected static List<MQClient> MQClients = new ArrayList<>();

    public static void add(MQClient MQClient) {
        MQClients.add(MQClient);
    }
}
