package org.carl.infrastructure.http;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HttpRequest {

    private final String method;
    private final URI uri;
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> queryParams;
    private final byte[] body;
    private final String contentType;
    private final Duration timeout;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.headers = copyMultiMap(builder.headers);
        this.queryParams = copyMultiMap(builder.queryParams);
        this.body = builder.body == null ? null : builder.body.clone();
        this.contentType = builder.contentType;
        this.timeout = builder.timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder get(String uri) {
        return builder().method("GET").uri(uri);
    }

    public static Builder post(String uri) {
        return builder().method("POST").uri(uri);
    }

    public static Builder put(String uri) {
        return builder().method("PUT").uri(uri);
    }

    public static Builder delete(String uri) {
        return builder().method("DELETE").uri(uri);
    }

    public String getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public byte[] getBody() {
        return body == null ? null : body.clone();
    }

    public String getContentType() {
        return contentType;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public boolean hasBody() {
        return body != null;
    }

    private static Map<String, List<String>> copyMultiMap(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        return Map.copyOf(copy);
    }

    public static final class Builder {

        private String method = "GET";
        private URI uri;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();
        private final Map<String, List<String>> queryParams = new LinkedHashMap<>();
        private byte[] body;
        private String contentType;
        private Duration timeout;

        public Builder method(String method) {
            String normalized = Objects.requireNonNull(method, "method").trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("method cannot be empty");
            }
            this.method = normalized.toUpperCase();
            return this;
        }

        public Builder uri(String uri) {
            return uri(URI.create(Objects.requireNonNull(uri, "uri")));
        }

        public Builder uri(URI uri) {
            this.uri = Objects.requireNonNull(uri, "uri");
            return this;
        }

        public Builder header(String name, String value) {
            add(headers, name, value);
            return this;
        }

        public Builder queryParam(String name, String value) {
            add(queryParams, name, value);
            return this;
        }

        public Builder body(String body) {
            return body(body, "text/plain; charset=UTF-8");
        }

        public Builder body(String body, String contentType) {
            Objects.requireNonNull(body, "body");
            return body(body.getBytes(StandardCharsets.UTF_8), contentType);
        }

        public Builder body(byte[] body) {
            return body(body, "application/octet-stream");
        }

        public Builder body(byte[] body, String contentType) {
            this.body = Objects.requireNonNull(body, "body").clone();
            this.contentType = Objects.requireNonNull(contentType, "contentType");
            return this;
        }

        public Builder timeout(Duration timeout) {
            Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        public HttpRequest build() {
            if (uri == null) {
                throw new IllegalStateException("uri is required");
            }
            return new HttpRequest(this);
        }

        private static void add(Map<String, List<String>> target, String name, String value) {
            String cleanName = Objects.requireNonNull(name, "name").trim();
            if (cleanName.isEmpty()) {
                throw new IllegalArgumentException("name cannot be empty");
            }
            target.computeIfAbsent(cleanName, ignored -> new ArrayList<>())
                    .add(Objects.requireNonNull(value, "value"));
        }
    }
}
