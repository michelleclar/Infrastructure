package org.carl.infra.mq.consumer;

/**
 * Provider-neutral dead letter policy for a consumer.
 *
 * <p>{@code maxRedeliverCount} is required and must be greater than zero. Topic names and the
 * initial subscription name are optional; when omitted, the provider applies its defaults.
 *
 * @param maxRedeliverCount maximum number of message redeliveries before dead-letter handling
 * @param retryLetterTopic retry topic to which failing messages are sent
 * @param deadLetterTopic dead letter topic to which exhausted messages are sent
 * @param initialSubscriptionName initial subscription to create for the dead letter topic; when
 *     omitted, no initial subscription is requested
 */
public record DeadLetterPolicy(
        int maxRedeliverCount,
        String retryLetterTopic,
        String deadLetterTopic,
        String initialSubscriptionName) {

    public DeadLetterPolicy {
        if (maxRedeliverCount <= 0) {
            throw new IllegalArgumentException("maxRedeliverCount must be greater than 0");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxRedeliverCount;
        private String retryLetterTopic;
        private String deadLetterTopic;
        private String initialSubscriptionName;

        private Builder() {}

        public Builder maxRedeliverCount(int maxRedeliverCount) {
            this.maxRedeliverCount = maxRedeliverCount;
            return this;
        }

        public Builder retryLetterTopic(String retryLetterTopic) {
            this.retryLetterTopic = retryLetterTopic;
            return this;
        }

        public Builder deadLetterTopic(String deadLetterTopic) {
            this.deadLetterTopic = deadLetterTopic;
            return this;
        }

        public Builder initialSubscriptionName(String initialSubscriptionName) {
            this.initialSubscriptionName = initialSubscriptionName;
            return this;
        }

        public DeadLetterPolicy build() {
            return new DeadLetterPolicy(
                    maxRedeliverCount,
                    retryLetterTopic,
                    deadLetterTopic,
                    initialSubscriptionName);
        }
    }
}
