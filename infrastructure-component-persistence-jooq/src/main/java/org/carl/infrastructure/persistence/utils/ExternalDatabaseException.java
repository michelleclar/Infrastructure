package org.carl.infrastructure.persistence.utils;

/**
 * Internal exception carrying a neutral failure category.
 */
public class ExternalDatabaseException extends RuntimeException {
    private final ExternalDatabaseFailureType failureType;

    public ExternalDatabaseException(ExternalDatabaseFailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public ExternalDatabaseException(
            ExternalDatabaseFailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    public ExternalDatabaseFailureType getFailureType() {
        return failureType;
    }
}
