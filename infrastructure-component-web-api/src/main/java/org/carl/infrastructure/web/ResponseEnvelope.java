package org.carl.infrastructure.web;

import java.util.List;
import java.util.Objects;

public record ResponseEnvelope<T>(
        boolean success,
        int code,
        String message,
        T data,
        String requestId,
        List<ValidationError> errors) {

    public ResponseEnvelope {
        message = Objects.requireNonNullElse(message, "");
        errors = List.copyOf(Objects.requireNonNullElse(errors, List.of()));
    }

    public static <T> ResponseEnvelope<T> ok(T data, WebRequestContext context) {
        return new ResponseEnvelope<>(true, 0, "OK", data, requestId(context), List.of());
    }

    public static ResponseEnvelope<Void> failure(WebError error, WebRequestContext context) {
        return new ResponseEnvelope<>(
                false,
                error.status(),
                error.message(),
                null,
                requestId(context),
                error.validationErrors());
    }

    private static String requestId(WebRequestContext context) {
        return context == null ? null : context.requestId();
    }
}
