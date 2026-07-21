package org.carl.infra.mq.pulsar.consumer;

import org.apache.pulsar.client.api.ConsumerBuilder;
import org.carl.infra.mq.consumer.ConsumerOption;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;

import java.util.Objects;
import java.util.function.Consumer;

/** Explicit access to Pulsar-native consumer settings. */
public final class PulsarConsumerOptions {

    private PulsarConsumerOptions() {}

    public static ConsumerOption configure(Consumer<ConsumerBuilder<?>> customizer) {
        return new NativeOption(Objects.requireNonNull(customizer, "customizer"));
    }

    public static void apply(ConsumerOption option, ConsumerBuilder<?> builder) {
        if (option instanceof NativeOption nativeOption) {
            nativeOption.customizer().accept(builder);
            return;
        }
        throw new UnsupportedMQCapabilityException("pulsar", "consumer option", option);
    }

    private record NativeOption(Consumer<ConsumerBuilder<?>> customizer)
            implements ConsumerOption {}
}
