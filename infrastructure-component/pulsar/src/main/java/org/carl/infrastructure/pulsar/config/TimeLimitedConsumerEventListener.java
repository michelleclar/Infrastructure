package org.carl.infrastructure.pulsar.config;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerEventListener;
import org.apache.pulsar.client.api.PulsarClientException;
import org.jboss.logging.Logger;

import java.util.concurrent.TimeUnit;

public class TimeLimitedConsumerEventListener implements ConsumerEventListener {
    private final long startTime;
    private final long maxDurationMs;
    private static final Logger logger = Logger.getLogger(TimeLimitedConsumerEventListener.class);

    public TimeLimitedConsumerEventListener(long duration, TimeUnit unit) {
        this.startTime = System.currentTimeMillis();
        this.maxDurationMs = unit.toMillis(duration);
    }

    @Override
    public void becameActive(Consumer<?> consumer, int partitionId) {
        checkTimeLimit(consumer);
    }

    @Override
    public void becameInactive(Consumer<?> consumer, int partitionId) {
        checkTimeLimit(consumer);
    }

    private void checkTimeLimit(Consumer<?> consumer) {
        if (System.currentTimeMillis() - startTime > maxDurationMs) {
            try {
                consumer.close();
                logger.info("Consumer auto-closed due to time limit");
            } catch (PulsarClientException e) {
                logger.error("Error auto-closing consumer", e);
            }
        }
    }
}
