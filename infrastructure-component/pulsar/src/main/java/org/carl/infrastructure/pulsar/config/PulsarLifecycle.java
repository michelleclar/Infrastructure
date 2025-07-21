package org.carl.infrastructure.pulsar.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.pulsar.client.api.PulsarClientException;
import org.carl.infrastructure.pulsar.factory.PulsarFactory;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PulsarLifecycle {
    @Inject MsgArgsConfig msgArgsConfig;
    GlobalShare globalShare = GlobalShare.getInstance();
    private static final Logger LOGGER = Logger.getLogger(PulsarLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        globalShare.msgArgsConfig = msgArgsConfig;
        new PulsarConfigValidator(globalShare.msgArgsConfig).validate();
        try {
            globalShare.pulsarClient = PulsarFactory.createClient(globalShare.msgArgsConfig);
            LOGGER.debugf("Pulsar client has been started successfully");
        } catch (PulsarClientException e) {
            LOGGER.errorf(
                    e,
                    "Pulsar client has been started failed: args:[%s], exception:[%s]",
                    msgArgsConfig,
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    void onStop(@Observes ShutdownEvent ev) throws PulsarClientException {
        globalShare.pulsarClient.close();
    }
}
