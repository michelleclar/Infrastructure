package org.carl.infra.mq.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.carl.infra.logging.ILogger;
import org.carl.infra.logging.LoggerFactory;
import org.carl.infra.mq.client.MQClient;
import org.carl.infra.mq.common.ex.MQClientException;

@ApplicationScoped
public class KafkaLifecycle {
    @Inject MQClient mqClient;
    private static final ILogger log = LoggerFactory.getLogger(KafkaLifecycle.class);

    void onStart(@Observes StartupEvent ev) {
        log.debug("Kafka MQ client has been started successfully");
    }

    void onStop(@Observes ShutdownEvent ev) throws MQClientException {
        mqClient.close();
    }
}
