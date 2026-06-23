package org.carl.infrastructure.web;

import java.util.Objects;

public record ValidationError(String field, String message, Object rejectedValue) {

    public ValidationError {
        field = normalize(field);
        message = Objects.requireNonNull(message, "message must not be null");
    }

    public static ValidationError of(String field, String message) {
        return new ValidationError(field, message, null);
    }

    public static ValidationError of(String field, String message, Object rejectedValue) {
        return new ValidationError(field, message, rejectedValue);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
