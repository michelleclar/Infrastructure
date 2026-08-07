package org.carl.infra.mq.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeadLetterPolicyTest {

    @Test
    void buildsImmutablePolicyWithAllFields() {
        DeadLetterPolicy policy =
                DeadLetterPolicy.builder()
                        .maxRedeliverCount(5)
                        .retryLetterTopic("persistent://public/default/orders-retry")
                        .deadLetterTopic("persistent://public/default/orders-dlq")
                        .initialSubscriptionName("orders-dlq-initial")
                        .build();

        assertEquals(5, policy.maxRedeliverCount());
        assertEquals("persistent://public/default/orders-retry", policy.retryLetterTopic());
        assertEquals("persistent://public/default/orders-dlq", policy.deadLetterTopic());
        assertEquals("orders-dlq-initial", policy.initialSubscriptionName());
    }

    @Test
    void allowsOptionalNamesToBeOmitted() {
        DeadLetterPolicy policy = DeadLetterPolicy.builder().maxRedeliverCount(5).build();

        assertNull(policy.retryLetterTopic());
        assertNull(policy.deadLetterTopic());
        assertNull(policy.initialSubscriptionName());
    }

    @Test
    void requiresPositiveMaxRedeliverCount() {
        assertThrows(IllegalArgumentException.class, () -> DeadLetterPolicy.builder().build());
        assertThrows(
                IllegalArgumentException.class,
                () -> DeadLetterPolicy.builder().maxRedeliverCount(-1).build());
    }
}
