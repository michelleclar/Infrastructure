package org.carl.infrastructure.logging;

/** Logging framework type enumeration */
public enum LoggingFramework {
    /** SLF4J logging framework */
    SLF4J,

    /** JBoss Logging framework (default for Quarkus) */
    JBOSS_LOGGING,

    /** Auto detect available logging framework */
    AUTO
}
