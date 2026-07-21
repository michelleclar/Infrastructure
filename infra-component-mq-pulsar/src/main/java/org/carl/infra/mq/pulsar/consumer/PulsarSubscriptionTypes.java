package org.carl.infra.mq.pulsar.consumer;

import org.carl.infra.mq.consumer.SubscriptionType;

/** Pulsar-native subscription distribution capabilities. */
public final class PulsarSubscriptionTypes {

    public static final SubscriptionType EXCLUSIVE = new NamedType("EXCLUSIVE");
    public static final SubscriptionType SHARED = new NamedType("SHARED");
    public static final SubscriptionType FAILOVER = new NamedType("FAILOVER");
    public static final SubscriptionType KEY_SHARED = new NamedType("KEY_SHARED");

    private PulsarSubscriptionTypes() {}

    private record NamedType(String name) implements SubscriptionType {
        @Override
        public String toString() {
            return name;
        }
    }
}
