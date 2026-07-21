package org.carl.infra.mq.pulsar.producer;

import org.carl.infra.mq.producer.HashingScheme;

/** Pulsar-native partition hashing capabilities. */
public final class PulsarHashingSchemes {

    public static final HashingScheme JAVA_STRING_HASH = new NamedScheme("JAVA_STRING_HASH");
    public static final HashingScheme MURMUR3_32_HASH = new NamedScheme("MURMUR3_32_HASH");

    private PulsarHashingSchemes() {}

    private record NamedScheme(String name) implements HashingScheme {
        @Override
        public String toString() {
            return name;
        }
    }
}
