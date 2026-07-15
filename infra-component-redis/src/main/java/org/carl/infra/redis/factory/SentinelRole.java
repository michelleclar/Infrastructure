package org.carl.infra.redis.factory;

public enum SentinelRole {
    /** Use a MASTER node connection. */
    MASTER,

    /** Use a REPLICA node connection. */
    REPLICA,

    /** Use a SENTINEL node connection. */
    SENTINEL
}
