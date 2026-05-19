package org.carl.infrastructure.http;

public final class HttpResponseContext {

    private final HttpRequest request;
    private final HttpResponse response;

    public HttpResponseContext(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public HttpResponse getResponse() {
        return response;
    }
}
