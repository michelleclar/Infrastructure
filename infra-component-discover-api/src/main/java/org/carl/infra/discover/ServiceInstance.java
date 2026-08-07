package org.carl.infra.discover;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** A healthy network endpoint returned by a service-discovery backend. */
public record ServiceInstance(
        String serviceName,
        String instanceId,
        String address,
        int port,
        Map<String, String> metadata) {

    public ServiceInstance {
        serviceName = requireText(serviceName, "serviceName");
        instanceId = requireText(instanceId, "instanceId");
        address = requireText(address, "address");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        metadata =
                Collections.unmodifiableMap(
                        new TreeMap<>(metadata == null ? Map.of() : metadata));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
