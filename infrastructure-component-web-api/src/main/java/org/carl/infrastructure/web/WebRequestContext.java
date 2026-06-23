package org.carl.infrastructure.web;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WebRequestContext(String requestId) {

    public WebRequestContext {
        requestId = normalizeRequestId(requestId);
    }

    public static WebRequestContext of(String requestId) {
        return new WebRequestContext(requestId);
    }

    public static WebRequestContext generated() {
        return new WebRequestContext(UUID.randomUUID().toString());
    }

    public Optional<String> requestIdValue() {
        return Optional.ofNullable(requestId);
    }

    private static String normalizeRequestId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String requireRequestId() {
        return Objects.requireNonNull(requestId, "requestId must not be null");
    }
}
