package org.carl.infra.discover;

/** Health-check definition attached to a service registration. */
public sealed interface HealthCheck
        permits NoHealthCheck, HttpHealthCheck, TcpHealthCheck, TtlHealthCheck {

    static HealthCheck none() {
        return NoHealthCheck.INSTANCE;
    }
}
