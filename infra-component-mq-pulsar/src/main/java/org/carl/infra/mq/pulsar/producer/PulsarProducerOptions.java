package org.carl.infra.mq.pulsar.producer;

import org.apache.pulsar.client.api.ProducerBuilder;
import org.carl.infra.mq.producer.ProducerOption;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;

import java.util.Objects;
import java.util.function.Consumer;

/** Explicit access to Pulsar-native producer settings. */
public final class PulsarProducerOptions {

    private PulsarProducerOptions() {}

    public static ProducerOption configure(Consumer<ProducerBuilder<?>> customizer) {
        return new NativeOption(Objects.requireNonNull(customizer, "customizer"));
    }

    public static void apply(ProducerOption option, ProducerBuilder<?> builder) {
        if (option instanceof NativeOption nativeOption) {
            nativeOption.customizer().accept(builder);
            return;
        }
        throw new UnsupportedMQCapabilityException("pulsar", "producer option", option);
    }

    private record NativeOption(Consumer<ProducerBuilder<?>> customizer)
            implements ProducerOption {}
}
