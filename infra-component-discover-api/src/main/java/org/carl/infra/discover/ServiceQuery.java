package org.carl.infra.discover;

/** Exact service lookup requested from a discovery backend. */
public record ServiceQuery(String serviceName, String tag) {

    public ServiceQuery {
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }
        tag = tag == null ? "" : tag;
    }

    public ServiceQuery(String serviceName) {
        this(serviceName, "");
    }
}
