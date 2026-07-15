package org.carl.infra.persistence.utils;

/**
 * Neutral failure categories for external database utility calls.
 */
public enum ExternalDatabaseFailureType {
    CONNECTION_FAILURE,
    TIMEOUT,
    SQL_EXECUTION_FAILURE,
    UNSUPPORTED_DATABASE_KIND,
    INVALID_CONFIGURATION
}
