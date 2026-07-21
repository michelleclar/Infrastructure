package org.carl.infra.mq.pulsar.producer;

import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.model.MessageBuilder;
import org.carl.infra.mq.model.MessageOption;

import java.time.Duration;
import java.util.Objects;

/** Pulsar-native per-message capabilities. */
public final class PulsarMessageOptions {

    private PulsarMessageOptions() {}

    public static MessageOption deliverAfter(Duration delay) {
        return new DeliverAfter(Objects.requireNonNull(delay, "delay"));
    }

    public static MessageOption deliverAt(long timestamp) {
        return new DeliverAt(timestamp);
    }

    public static MessageOption sequenceId(long sequenceId) {
        return new SequenceId(sequenceId);
    }

    public static MessageOption disableReplication() {
        return DisableReplication.INSTANCE;
    }

    public static void apply(MessageOption option, MessageBuilder<?> builder) {
        if (option instanceof DeliverAfter deliverAfter) {
            builder.deliverAfter(deliverAfter.delay().toMillis());
        } else if (option instanceof DeliverAt deliverAt) {
            builder.deliverAt(deliverAt.timestamp());
        } else if (option instanceof SequenceId sequenceId) {
            builder.sequenceId(sequenceId.value());
        } else if (option == DisableReplication.INSTANCE) {
            builder.disableReplication();
        } else {
            throw new UnsupportedMQCapabilityException("pulsar", "message option", option);
        }
    }

    private record DeliverAfter(Duration delay) implements MessageOption {}

    private record DeliverAt(long timestamp) implements MessageOption {}

    private record SequenceId(long value) implements MessageOption {}

    private static final class DisableReplication implements MessageOption {
        private static final DisableReplication INSTANCE = new DisableReplication();

        private DisableReplication() {}
    }
}
