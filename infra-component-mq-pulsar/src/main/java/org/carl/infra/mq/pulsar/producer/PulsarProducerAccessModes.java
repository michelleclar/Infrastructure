package org.carl.infra.mq.pulsar.producer;

import org.carl.infra.mq.producer.ProducerAccessMode;

/** Pulsar-native producer access capabilities. */
public final class PulsarProducerAccessModes {

    public static final ProducerAccessMode EXCLUSIVE = new NamedMode("EXCLUSIVE");
    public static final ProducerAccessMode EXCLUSIVE_WITH_FENCING =
            new NamedMode("EXCLUSIVE_WITH_FENCING");
    public static final ProducerAccessMode WAIT_FOR_EXCLUSIVE =
            new NamedMode("WAIT_FOR_EXCLUSIVE");

    private PulsarProducerAccessModes() {}

    private record NamedMode(String name) implements ProducerAccessMode {
        @Override
        public String toString() {
            return name;
        }
    }
}
