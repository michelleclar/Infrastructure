package org.carl.infrastructure.pulsar.builder;

public enum CompressionType {
    /** No compression. */
    NONE,

    /** Compress with LZ4 algorithm. Faster but lower compression than ZLib. */
    LZ4,

    /** Compress with ZLib. */
    ZLIB,

    /** Compress with Zstandard codec. */
    ZSTD,

    /** Compress with Snappy codec. */
    SNAPPY
}
