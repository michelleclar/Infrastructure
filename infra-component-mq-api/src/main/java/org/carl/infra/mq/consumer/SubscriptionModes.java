package org.carl.infra.mq.consumer;

/** Provider-neutral subscription persistence capabilities. */
public final class SubscriptionModes {

    /** Persist subscription progress so consumption can resume after a restart. */
    public static final SubscriptionMode DURABLE = new NamedSubscriptionMode("DURABLE");

    private SubscriptionModes() {}

    private record NamedSubscriptionMode(String name) implements SubscriptionMode {
        @Override
        public String toString() {
            return name;
        }
    }
}
