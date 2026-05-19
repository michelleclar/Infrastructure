package org.carl.infrastructure.http;

import java.util.Optional;

public final class HttpCompletionContext {

    private final HttpRequest request;
    private final HttpResponse response;
    private final Throwable error;

    public HttpCompletionContext(HttpRequest request, HttpResponse response, Throwable error) {
        this.request = request;
        this.response = response;
        this.error = error;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public Optional<HttpResponse> getResponse() {
        return Optional.ofNullable(response);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }
}
