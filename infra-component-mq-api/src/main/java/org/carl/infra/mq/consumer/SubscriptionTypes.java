package org.carl.infra.mq.consumer;

/** Provider-neutral subscription capabilities. */
public final class SubscriptionTypes {

    /**
     * Consumers sharing one subscription distribute work between them. The contract intentionally
     * does not prescribe whether distribution happens per message or per partition.
     */
    public static final SubscriptionType LOAD_BALANCED = new NamedSubscriptionType("LOAD_BALANCED");

    private SubscriptionTypes() {}

    private record NamedSubscriptionType(String name) implements SubscriptionType {
        @Override
        public String toString() {
            return name;
        }
    }
}
