package org.carl.infra.http;

public final class HttpRequestContext {

    private final HttpRequest request;

    public HttpRequestContext(HttpRequest request) {
        this.request = request;
    }

    public HttpRequest getRequest() {
        return request;
    }
}
