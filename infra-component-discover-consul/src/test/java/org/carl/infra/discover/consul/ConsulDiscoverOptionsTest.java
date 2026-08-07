package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.net.URI;

class ConsulDiscoverOptionsTest {

    @Test
    void redactsAclToken() {
        ConsulDiscoverOptions options =
                ConsulDiscoverOptions.builder(URI.create("http://localhost:8500"), "config/orders")
                        .aclToken("top-secret-token")
                        .build();

        assertFalse(options.toString().contains("top-secret-token"));
        assertTrue(options.initialConfigRequired());
    }

    @Test
    void requiresExplicitConfigKey() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ConsulDiscoverOptions.builder(
                                        URI.create("http://localhost:8500"), " ")
                                .build());
    }
}
