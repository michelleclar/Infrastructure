package org.carl.infrastructure.http;

public final class HttpErrorContext {

    private final HttpRequest request;
    private final Throwable error;

    public HttpErrorContext(HttpRequest request, Throwable error) {
        this.request = request;
        this.error = error;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public Throwable getError() {
        return error;
    }
}
