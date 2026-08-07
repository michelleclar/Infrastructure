package org.carl.infra.discover;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** HTTP health check executed by Consul. */
public record HttpHealthCheck(
        URI uri,
        Duration interval,
        Duration deregisterAfter,
        boolean tlsSkipVerify,
        Map<String, List<String>> headers)
        implements HealthCheck {

    public HttpHealthCheck {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("uri must be an absolute HTTP(S) URI");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("uri scheme must be http or https");
        }
        interval = requirePositive(interval, "interval");
        deregisterAfter = requirePositive(deregisterAfter, "deregisterAfter");
        Map<String, List<String>> headerCopy = new TreeMap<>();
        if (headers != null) {
            headers.forEach(
                    (name, values) ->
                            headerCopy.put(
                                    name,
                                    Collections.unmodifiableList(
                                            new ArrayList<>(
                                                    values == null ? List.of() : values))));
        }
        headers = Collections.unmodifiableMap(headerCopy);
    }

    public HttpHealthCheck(URI uri, Duration interval, Duration deregisterAfter) {
        this(uri, interval, deregisterAfter, false, Map.of());
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
