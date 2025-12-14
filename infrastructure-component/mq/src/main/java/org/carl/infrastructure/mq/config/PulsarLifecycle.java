package org.carl.infrastructure.mq.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.carl.infrastructure.logging.ILogger;
import org.carl.infrastructure.logging.LoggerFactory;
import org.carl.infrastructure.mq.client.MQClient;
import org.carl.infrastructure.mq.common.ex.MQClientException;

@ApplicationScoped
public class PulsarLifecycle {
    @Inject MQClient mqClient;
    private static final ILogger log = LoggerFactory.getLogger(PulsarLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        log.debug("MQ client has been started successfully");
    }

    void onStop(@Observes ShutdownEvent ev) throws MQClientException {
        mqClient.close();
    }
}
