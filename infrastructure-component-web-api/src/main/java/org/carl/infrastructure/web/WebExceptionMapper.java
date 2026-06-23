package org.carl.infrastructure.web;

import java.util.Objects;

public class WebExceptionMapper<T> {

    private final ResponseEnvelopeAdapter<T> adapter;

    public WebExceptionMapper(ResponseEnvelopeAdapter<T> adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
    }

    public MappedWebResponse<T> map(Throwable throwable, WebRequestContext context) {
        WebError error = resolveError(throwable);
        return new MappedWebResponse<>(error.status(), adapter.failure(error, context));
    }

    private WebError resolveError(Throwable throwable) {
        if (throwable instanceof WebApiException webApiException) {
            return webApiException.error();
        }
        String message = throwable == null ? "Unhandled exception" : throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = "Unhandled exception";
        }
        return WebError.internal(message);
    }
}
