package org.carl.infrastructure.broadcast.core;

import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class BroadcastContextProvider implements Provider<BroadcastContext> {
    @Inject EventBus eventBus;

    @Produces
    @ApplicationScoped
    @Override
    public BroadcastContext get() {
        Config config = ConfigProvider.getConfig();
        String serviceUrl = config.getValue("mp.messaging.incoming.data.serviceUrl", String.class);
        ClientBuilder clientBuilder = PulsarClient.builder().serviceUrl(serviceUrl);
        return new BroadcastContext(clientBuilder, eventBus);
    }
}
