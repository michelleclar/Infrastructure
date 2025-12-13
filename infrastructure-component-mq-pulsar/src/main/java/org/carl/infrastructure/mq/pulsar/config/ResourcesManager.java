package org.carl.infrastructure.mq.pulsar.config;

import org.apache.pulsar.client.api.PulsarClient;

import java.util.ArrayList;
import java.util.List;

public class ResourcesManager {
    protected static List<PulsarClient> pulsarClients = new ArrayList<>();

    public static void add(PulsarClient pulsarClient) {
        pulsarClients.add(pulsarClient);
    }
}
