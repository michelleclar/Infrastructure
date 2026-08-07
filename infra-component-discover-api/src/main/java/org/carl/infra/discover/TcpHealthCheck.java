package org.carl.infra.discover;

import java.time.Duration;

/** TCP health check executed by Consul. */
public record TcpHealthCheck(String target, Duration interval, Duration deregisterAfter)
        implements HealthCheck {

    public TcpHealthCheck {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("target must not be blank");
        }
        interval = requirePositive(interval, "interval");
        deregisterAfter = requirePositive(deregisterAfter, "deregisterAfter");
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
