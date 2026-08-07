package org.carl.infra.discover;

import java.time.Duration;

/** TTL health check refreshed by the registered service. */
public record TtlHealthCheck(Duration ttl, Duration deregisterAfter) implements HealthCheck {

    public TtlHealthCheck {
        ttl = requirePositive(ttl, "ttl");
        deregisterAfter = requirePositive(deregisterAfter, "deregisterAfter");
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
