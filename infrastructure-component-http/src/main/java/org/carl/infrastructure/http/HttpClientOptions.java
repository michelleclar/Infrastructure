package org.carl.infrastructure.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HttpClientOptions {

    private final Duration connectTimeout;
    private final Duration responseTimeout;
    private final int maxConnections;
    private final List<HttpInterceptor> interceptors;

    private HttpClientOptions(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.responseTimeout = builder.responseTimeout;
        this.maxConnections = builder.maxConnections;
        this.interceptors = List.copyOf(builder.interceptors);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public List<HttpInterceptor> getInterceptors() {
        return interceptors;
    }

    public static final class Builder {

        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration responseTimeout = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private final List<HttpInterceptor> interceptors = new ArrayList<>();

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = requirePositive(connectTimeout, "connectTimeout");
            return this;
        }

        public Builder responseTimeout(Duration responseTimeout) {
            this.responseTimeout = requirePositive(responseTimeout, "responseTimeout");
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            if (maxConnections <= 0) {
                throw new IllegalArgumentException("maxConnections must be positive");
            }
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder addInterceptor(HttpInterceptor interceptor) {
            this.interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        public Builder interceptors(List<HttpInterceptor> interceptors) {
            this.interceptors.clear();
            this.interceptors.addAll(Objects.requireNonNull(interceptors, "interceptors"));
            return this;
        }

        public HttpClientOptions build() {
            return new HttpClientOptions(this);
        }

        private static Duration requirePositive(Duration duration, String name) {
            Objects.requireNonNull(duration, name);
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return duration;
        }
    }
}
