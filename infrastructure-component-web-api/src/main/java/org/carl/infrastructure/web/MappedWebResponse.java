package org.carl.infrastructure.web;

import java.util.Objects;

public record MappedWebResponse<T>(int status, T body) {

    public MappedWebResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be a valid HTTP status");
        }
        Objects.requireNonNull(body, "body must not be null");
    }
}
