package org.carl.infrastructure.workflowv2.api;

import java.time.Duration;

/**
 * Vendor-neutral retry policy for a {@link Node}. The engine maps it to Temporal {@code
 * RetryOptions}; business code never imports {@code io.temporal.*}.
 */
public final class RetryPolicy {

    private final int maxAttempts;
    private final Duration initialInterval;
    private final double backoffCoefficient;
    private final Duration maximumInterval;

    private RetryPolicy(
            int maxAttempts,
            Duration initialInterval,
            double backoffCoefficient,
            Duration maximumInterval) {
        this.maxAttempts = maxAttempts;
        this.initialInterval = initialInterval;
        this.backoffCoefficient = backoffCoefficient;
        this.maximumInterval = maximumInterval;
    }

    /** Policy with sensible defaults (1s initial, x2 backoff, 60s cap). */
    public static RetryPolicy of(int maxAttempts) {
        return new RetryPolicy(maxAttempts, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(60));
    }

    public static RetryPolicy of(
            int maxAttempts,
            Duration initialInterval,
            double backoffCoefficient,
            Duration maximumInterval) {
        return new RetryPolicy(maxAttempts, initialInterval, backoffCoefficient, maximumInterval);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration initialInterval() {
        return initialInterval;
    }

    public double backoffCoefficient() {
        return backoffCoefficient;
    }

    public Duration maximumInterval() {
        return maximumInterval;
    }
}
