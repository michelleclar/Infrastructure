package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.ext.consul.CheckOptions;

import org.carl.infra.discover.HealthCheck;
import org.carl.infra.discover.HttpHealthCheck;
import org.carl.infra.discover.ServiceRegistration;
import org.carl.infra.discover.TcpHealthCheck;
import org.carl.infra.discover.TtlHealthCheck;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class ConsulHealthCheckMappingTest {

    @Test
    void mapsHttpTcpAndTtlChecksExactly() {
        CheckOptions http =
                ConsulDiscoverClient.toCheckOptions(
                        registration(
                                new HttpHealthCheck(
                                        URI.create("https://service.example/health"),
                                        Duration.ofSeconds(10),
                                        Duration.ofMinutes(1),
                                        true,
                                        Map.of("X-Probe", List.of("yes")))));
        assertEquals("https://service.example/health", http.getHttp());
        assertEquals("10s", http.getInterval());
        assertEquals("1m", http.getDeregisterAfter());
        assertTrue(http.isTlsSkipVerify());
        assertEquals(List.of("yes"), http.getHeaders().get("X-Probe"));

        CheckOptions tcp =
                ConsulDiscoverClient.toCheckOptions(
                        registration(
                                new TcpHealthCheck(
                                        "10.0.0.8:8080",
                                        Duration.ofSeconds(5),
                                        Duration.ofSeconds(30))));
        assertEquals("10.0.0.8:8080", tcp.getTcp());
        assertEquals("5s", tcp.getInterval());
        assertEquals("30s", tcp.getDeregisterAfter());

        CheckOptions ttl =
                ConsulDiscoverClient.toCheckOptions(
                        registration(
                                new TtlHealthCheck(
                                        Duration.ofSeconds(15), Duration.ofMinutes(2))));
        assertEquals("15s", ttl.getTtl());
        assertEquals("2m", ttl.getDeregisterAfter());
        assertEquals("service:orders-1", ttl.getId());

        assertNull(
                ConsulDiscoverClient.toCheckOptions(registration(HealthCheck.none())));
    }

    private static ServiceRegistration registration(HealthCheck healthCheck) {
        return new ServiceRegistration(
                "orders",
                "orders-1",
                "10.0.0.8",
                8080,
                List.of(),
                Map.of(),
                healthCheck);
    }
}
