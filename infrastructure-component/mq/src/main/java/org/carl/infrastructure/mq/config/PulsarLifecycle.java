package org.carl.infrastructure.mq.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;

@ApplicationScoped
public class PulsarLifecycle {
    @Inject PulsarClient pulsarClient;
    private static final ILogger log = LoggerFactory.getLogger(PulsarLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        log.debug("Pulsar client has been started successfully");
    }

    void onStop(@Observes ShutdownEvent ev) throws PulsarClientException {
        pulsarClient.close();
    }
}
