package org.carl.infrastructure.mq.producer;

/**
 * Standard hashing functions available when choosing the partition to use for a particular message.
 */
public enum HashingScheme {

    /** Use regular <code>String.hashCode()</code>. */
    JavaStringHash,

    /**
     * Use Murmur3 hashing function. <a
     * href="https://en.wikipedia.org/wiki/MurmurHash">https://en.wikipedia.org/wiki/MurmurHash</a>
     */
    Murmur3_32Hash
}
