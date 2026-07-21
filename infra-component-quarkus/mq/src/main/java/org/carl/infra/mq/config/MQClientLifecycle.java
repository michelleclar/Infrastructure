package org.carl.infra.mq.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;

/** Manages the lifecycle of the MQ client supplied by the selected Quarkus provider module. */
@ApplicationScoped
public class MQClientLifecycle {
    private static final ILogger log = LoggerFactory.getLogger(MQClientLifecycle.class);

    @Inject Instance<MQClient> mqClient;

    void onStart(@Observes StartupEvent event) {
        if (mqClient.isUnsatisfied()) {
            throw new IllegalStateException(
                    "No MQClient bean found. Add exactly one Quarkus MQ provider module");
        }
        if (mqClient.isAmbiguous()) {
            throw new IllegalStateException(
                    "Multiple MQClient beans found. Keep exactly one Quarkus MQ provider module");
        }
        mqClient.get();
        log.debug("MQ client has been started successfully");
    }

    void onStop(@Observes ShutdownEvent event) throws MQClientException {
        if (mqClient.isResolvable()) {
            mqClient.get().close();
        }
    }
}
