package org.carl.infrastructure.web;

import java.util.List;
import java.util.Objects;

public record WebError(
        int status,
        String code,
        String message,
        String errorType,
        String scenario,
        List<ValidationError> validationErrors) {

    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public WebError {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be a valid HTTP status");
        }
        code = normalizeCode(code);
        message = Objects.requireNonNullElse(message, code);
        errorType = Objects.requireNonNullElse(errorType, code);
        scenario = Objects.requireNonNullElse(scenario, code);
        validationErrors = List.copyOf(Objects.requireNonNullElse(validationErrors, List.of()));
    }

    public static WebError business(String message) {
        return new WebError(400, BUSINESS_ERROR, message, BUSINESS_ERROR, BUSINESS_ERROR, List.of());
    }

    public static WebError business(int status, String code, String message) {
        return new WebError(status, code, message, BUSINESS_ERROR, code, List.of());
    }

    public static WebError validation(List<ValidationError> errors) {
        return new WebError(
                400,
                VALIDATION_ERROR,
                "Validation failed",
                VALIDATION_ERROR,
                VALIDATION_ERROR,
                errors);
    }

    public static WebError notFound(String message) {
        return new WebError(404, NOT_FOUND, message, NOT_FOUND, NOT_FOUND, List.of());
    }

    public static WebError internal(String message) {
        return new WebError(500, INTERNAL_ERROR, message, INTERNAL_ERROR, INTERNAL_ERROR, List.of());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return INTERNAL_ERROR;
        }
        return value;
    }
}
