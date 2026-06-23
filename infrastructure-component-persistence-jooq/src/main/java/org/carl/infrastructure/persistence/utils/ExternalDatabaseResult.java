package org.carl.infrastructure.persistence.utils;

import java.util.Objects;

/**
 * Neutral success/failure wrapper for external database utility calls.
 */
public record ExternalDatabaseResult<T>(
        boolean success,
        T value,
        ExternalDatabaseFailureType failureType,
        String message,
        String exceptionType) {

    public static <T> ExternalDatabaseResult<T> success(T value) {
        return new ExternalDatabaseResult<>(true, value, null, null, null);
    }

    public static <T> ExternalDatabaseResult<T> failure(
            ExternalDatabaseFailureType failureType, String message, Throwable throwable) {
        Objects.requireNonNull(failureType);
        String exceptionType = throwable == null ? null : throwable.getClass().getName();
        return new ExternalDatabaseResult<>(false, null, failureType, message, exceptionType);
    }
}
