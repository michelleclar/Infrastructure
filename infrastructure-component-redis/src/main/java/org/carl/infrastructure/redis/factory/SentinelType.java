package org.carl.infrastructure.redis.factory;

public enum SentinelType {

    /** The client should work in single server mode (the default). */
    STANDALONE,

    /**
     * The client should work in sentinel mode. When this mode is active, use the {@link RedisRole}
     * to define which role to get the client connection to.
     */
    SENTINEL,

    /**
     * The client should work in cluster mode. When this mode is active, use the {@link
     * RedisReplicas} to define when replica nodes can be used for read only queries, and use {@link
     * RedisClusterTransactions} to define how transactions should be handled.
     */
    CLUSTER,

    /**
     * The client should work in replication mode. When this mode is active, use the {@link
     * RedisReplicas} to define when replica nodes can be used for read only queries.
     */
    REPLICATION
}
