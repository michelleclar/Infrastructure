package org.carl.infrastructure.web;

import java.io.Serial;
import java.util.Objects;

public class WebApiException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final WebError error;

    public WebApiException(WebError error) {
        super(Objects.requireNonNull(error, "error must not be null").message());
        this.error = error;
    }

    public WebApiException(WebError error, Throwable cause) {
        super(Objects.requireNonNull(error, "error must not be null").message(), cause);
        this.error = error;
    }

    public WebError error() {
        return error;
    }
}
