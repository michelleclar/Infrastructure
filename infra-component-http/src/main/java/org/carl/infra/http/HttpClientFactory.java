package org.carl.infra.http;

import org.carl.infra.http.apache.ApacheAsyncHttpClient;

public final class HttpClientFactory {

    private HttpClientFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static HttpClient create() {
        return create(HttpClientOptions.builder().build());
    }

    public static HttpClient create(HttpClientOptions options) {
        return new ApacheAsyncHttpClient(options);
    }
}
