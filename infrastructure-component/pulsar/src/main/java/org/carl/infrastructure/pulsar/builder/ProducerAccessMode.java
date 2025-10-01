package org.carl.infrastructure.pulsar.builder;

/** The type of access to the topic that the producer requires. */
public enum ProducerAccessMode {
    /** By default multiple producers can publish on a topic. */
    Shared,

    /**
     * Require exclusive access for producer. Fail immediately if there's already a producer
     * connected.
     */
    Exclusive,

    /**
     * Acquire exclusive access for the producer. Any existing producer will be removed and
     * invalidated immediately.
     */
    ExclusiveWithFencing,

    /** Producer creation is pending until it can acquire exclusive access. */
    WaitForExclusive,
}
