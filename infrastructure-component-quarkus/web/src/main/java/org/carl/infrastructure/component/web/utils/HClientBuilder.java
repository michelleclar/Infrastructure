package org.carl.infrastructure.component.web.utils;

import io.quarkus.vertx.web.Route;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HClientBuilder {
    private final HClient hClient;

    public static HClientBuilder create(String url) {
        return new HClientBuilder(new HClient(url));
    }

    public HClientBuilder(HClient hClient) {
        this.hClient = hClient;
    }

    public HClientBuilder entity(Object entity) {
        this.hClient.entity = entity;
        return this;
    }

    public HClientBuilder headers(Map<String, String> headers) {
        this.hClient.headers = headers;
        return this;
    }

    public HClientBuilder headers(Consumer<Map<String, String>> headers) {
        Map<String, String> headersMap =
                this.hClient.headers == null ? new HashMap<>() : this.hClient.headers;
        headers.accept(headersMap);
        this.hClient.headers = headersMap;
        return this;
    }

    public HClientBuilder method(Route.HttpMethod requestMethod) {
        this.hClient.requestMethod = requestMethod;
        return this;
    }

    public HClient build() {
        // TODO: add check
        return hClient;
    }
}
