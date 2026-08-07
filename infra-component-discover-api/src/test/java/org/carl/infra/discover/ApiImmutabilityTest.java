package org.carl.infra.discover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class ApiImmutabilityTest {

    @Test
    void serviceInstanceDefensivelyCopiesMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("zone", "a");

        ServiceInstance instance = new ServiceInstance("orders", "orders-1", "10.0.0.1", 8080, metadata);
        metadata.put("zone", "b");

        assertEquals("a", instance.metadata().get("zone"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> instance.metadata().put("zone", "c"));
    }
}
