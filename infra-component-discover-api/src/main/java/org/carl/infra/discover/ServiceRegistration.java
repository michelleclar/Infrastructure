package org.carl.infra.discover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Complete, explicit service registration. */
public record ServiceRegistration(
        String serviceName,
        String instanceId,
        String address,
        int port,
        List<String> tags,
        Map<String, String> metadata,
        HealthCheck healthCheck) {

    public ServiceRegistration {
        serviceName = requireText(serviceName, "serviceName");
        instanceId = requireText(instanceId, "instanceId");
        address = requireText(address, "address");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        tags =
                Collections.unmodifiableList(
                        new ArrayList<>(tags == null ? List.of() : tags));
        metadata =
                Collections.unmodifiableMap(
                        new TreeMap<>(metadata == null ? Map.of() : metadata));
        healthCheck = healthCheck == null ? HealthCheck.none() : healthCheck;
    }

    public ServiceRegistration(
            String serviceName, String instanceId, String address, int port) {
        this(serviceName, instanceId, address, port, List.of(), Map.of(), HealthCheck.none());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
