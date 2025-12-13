package org.carl.infrastructure.mq.consumer;

public enum SubscriptionInitialPosition {
    /** The latest position which means the start consuming position will be the last message. */
    Latest(0),

    /** The earliest position which means the start consuming position will be the first message. */
    Earliest(1);

    private final int value;

    SubscriptionInitialPosition(int value) {
        this.value = value;
    }

    public final int getValue() {
        return value;
    }
}
