package org.carl.infra.mq.kafka.builder;

import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.consumer.SubscriptionModes;
import org.carl.infra.mq.consumer.ConsumerOption;
import org.carl.infra.mq.consumer.SubscriptionMode;
import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.kafka.config.KafkaConfig;
import org.carl.infra.mq.producer.ProducerAccessModes;
import org.carl.infra.mq.producer.ProducerAccessMode;
import org.carl.infra.mq.producer.ProducerOption;
import org.carl.infra.mq.model.MessageOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaCapabilityBoundaryTest {

    private final KafkaConfig config = new KafkaConfig("localhost:9092");
    private final KafkaMQClient client = new KafkaMQClient(config);

    @Test
    void acceptsOnlyPortableSubscriptionCapabilities() {
        KafkaConsumerBuilder<byte[]> builder =
                KafkaConsumerBuilder.create(client, config.consumer());

        assertDoesNotThrow(() -> builder.subscriptionType(SubscriptionTypes.LOAD_BALANCED));
        assertDoesNotThrow(() -> builder.subscriptionMode(SubscriptionModes.DURABLE));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> builder.subscriptionType(new SubscriptionType() {}));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> builder.subscriptionMode(new SubscriptionMode() {}));
    }

    @Test
    void rejectsUnsupportedSubscriptionTypeLoadedFromConfiguration() {
        ((KafkaConfig.KafkaConsumerConfig) config.consumer())
                .setSubscriptionType(new SubscriptionType() {});

        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> KafkaConsumerBuilder.create(client, config.consumer()));
    }

    @Test
    void acceptsOnlyPortableProducerAccessCapability() {
        KafkaProducerBuilder<byte[]> builder = KafkaProducerBuilder.create(client, config.producer());

        assertDoesNotThrow(() -> builder.accessMode(ProducerAccessModes.SHARED));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> builder.accessMode(new ProducerAccessMode() {}));
    }

    @Test
    void rejectsPreviouslyIgnoredPulsarSettings() {
        KafkaConsumerBuilder<byte[]> consumerBuilder =
                KafkaConsumerBuilder.create(client, config.consumer());
        KafkaProducerBuilder<byte[]> producerBuilder =
                KafkaProducerBuilder.create(client, config.producer());

        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> consumerBuilder.readCompacted(true));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> consumerBuilder.enableRetry(true));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> producerBuilder.enableChunking(true));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> producerBuilder.initialSequenceId(1));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> consumerBuilder.option(new ConsumerOption() {}));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> producerBuilder.option(new ProducerOption() {}));
        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> new KafkaMessageBuilder<>("value").option(new MessageOption() {}));
    }
}
