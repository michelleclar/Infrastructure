package org.carl.infra.mq.pulsar.consumer;

import org.carl.infra.mq.consumer.SubscriptionMode;

/** Pulsar-native subscription persistence capabilities. */
public final class PulsarSubscriptionModes {

    public static final SubscriptionMode NON_DURABLE = new NamedMode("NON_DURABLE");

    private PulsarSubscriptionModes() {}

    private record NamedMode(String name) implements SubscriptionMode {
        @Override
        public String toString() {
            return name;
        }
    }
}
