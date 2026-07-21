package org.carl.infra.mq.pulsar.builder;

import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infra.mq.common.ex.UnsupportedMQCapabilityException;
import org.carl.infra.mq.consumer.SubscriptionType;
import org.carl.infra.mq.consumer.SubscriptionTypes;
import org.carl.infra.mq.producer.ProducerAccessMode;
import org.carl.infra.mq.producer.ProducerAccessModes;
import org.carl.infra.mq.pulsar.config.PulsarConfig;
import org.carl.infra.mq.pulsar.consumer.PulsarSubscriptionTypes;
import org.carl.infra.mq.pulsar.producer.PulsarProducerAccessModes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PulsarCapabilityMappingTest {

    @Test
    void mapsPortableAndPulsarSubscriptionTypesExplicitly() {
        Map<String, Object> calls = new HashMap<>();
        ConsumerBuilder<?> nativeBuilder = fluentProxy(ConsumerBuilder.class, calls);
        PulsarClient client = clientProxy(nativeBuilder, null);
        PulsarConfig config = new PulsarConfig("pulsar://localhost:6650");
        PulsarConsumerBuilder<byte[]> builder =
                PulsarConsumerBuilder.create(client, config.consumer());

        builder.subscriptionType(SubscriptionTypes.LOAD_BALANCED);
        assertEquals(
                org.apache.pulsar.client.api.SubscriptionType.Shared,
                calls.get("subscriptionType"));

        builder.subscriptionType(PulsarSubscriptionTypes.KEY_SHARED);
        assertEquals(
                org.apache.pulsar.client.api.SubscriptionType.Key_Shared,
                calls.get("subscriptionType"));

        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> builder.subscriptionType(new SubscriptionType() {}));
    }

    @Test
    void mapsPortableAndPulsarProducerAccessModesExplicitly() {
        Map<String, Object> calls = new HashMap<>();
        ProducerBuilder<?> nativeBuilder = fluentProxy(ProducerBuilder.class, calls);
        PulsarClient client = clientProxy(null, nativeBuilder);
        PulsarConfig config = new PulsarConfig("pulsar://localhost:6650");
        PulsarProducerBuilder<byte[]> builder =
                PulsarProducerBuilder.create(client, config.producer());

        builder.accessMode(ProducerAccessModes.SHARED);
        assertEquals(
                org.apache.pulsar.client.api.ProducerAccessMode.Shared,
                calls.get("accessMode"));

        builder.accessMode(PulsarProducerAccessModes.EXCLUSIVE_WITH_FENCING);
        assertEquals(
                org.apache.pulsar.client.api.ProducerAccessMode.ExclusiveWithFencing,
                calls.get("accessMode"));

        assertThrows(
                UnsupportedMQCapabilityException.class,
                () -> builder.accessMode(new ProducerAccessMode() {}));
    }

    @SuppressWarnings("unchecked")
    private static <T> T fluentProxy(Class<T> type, Map<String, Object> calls) {
        Object[] holder = new Object[1];
        holder[0] =
                Proxy.newProxyInstance(
                        type.getClassLoader(),
                        new Class<?>[] {type},
                        (proxy, method, args) -> {
                            if (args != null && args.length == 1) {
                                calls.put(method.getName(), args[0]);
                            }
                            if (method.getReturnType().isInstance(holder[0])) {
                                return holder[0];
                            }
                            return defaultValue(method.getReturnType());
                        });
        return (T) holder[0];
    }

    private static PulsarClient clientProxy(
            ConsumerBuilder<?> consumerBuilder, ProducerBuilder<?> producerBuilder) {
        return (PulsarClient)
                Proxy.newProxyInstance(
                        PulsarClient.class.getClassLoader(),
                        new Class<?>[] {PulsarClient.class},
                        (proxy, method, args) ->
                                switch (method.getName()) {
                                    case "newConsumer" -> consumerBuilder;
                                    case "newProducer" -> producerBuilder;
                                    default -> defaultValue(method.getReturnType());
                                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
