package org.carl.infra.mq.pulsar.producer;

import org.carl.infra.mq.producer.MessageRoutingMode;

/** Pulsar-native message-routing capabilities. */
public final class PulsarMessageRoutingModes {

    public static final MessageRoutingMode SINGLE_PARTITION = new NamedMode("SINGLE_PARTITION");
    public static final MessageRoutingMode ROUND_ROBIN_PARTITION =
            new NamedMode("ROUND_ROBIN_PARTITION");
    public static final MessageRoutingMode CUSTOM_PARTITION = new NamedMode("CUSTOM_PARTITION");

    private PulsarMessageRoutingModes() {}

    private record NamedMode(String name) implements MessageRoutingMode {
        @Override
        public String toString() {
            return name;
        }
    }
}
