package org.carl.infra.discover.consul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;

import org.carl.infra.discover.ServiceInstance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ConsulServiceInstancesTest {

    @Test
    void sortsDeduplicatesAndReturnsImmutableSnapshot() {
        ServiceEntry duplicateWithHigherAddress =
                entry("orders", "orders-1", "10.0.0.9", 8080, Map.of("zone", "b"));
        ServiceEntry second =
                entry("orders", "orders-2", "10.0.0.2", 8080, Map.of("zone", "a"));
        ServiceEntry duplicateWithLowerAddress =
                entry("orders", "orders-1", "10.0.0.1", 8080, Map.of("zone", "a"));

        List<ServiceInstance> instances =
                ConsulDiscoverClient.mapInstances(
                        new ServiceEntryList()
                                .setList(
                                        List.of(
                                                duplicateWithHigherAddress,
                                                second,
                                                duplicateWithLowerAddress)));

        assertEquals(List.of("orders-1", "orders-2"), instances.stream()
                .map(ServiceInstance::instanceId)
                .toList());
        assertEquals("10.0.0.1", instances.getFirst().address());
        assertThrows(
                UnsupportedOperationException.class,
                () -> instances.add(instances.getFirst()));
    }

    private static ServiceEntry entry(
            String serviceName,
            String instanceId,
            String address,
            int port,
            Map<String, String> metadata) {
        return new ServiceEntry()
                .setService(
                        new Service()
                                .setName(serviceName)
                                .setId(instanceId)
                                .setAddress(address)
                                .setPort(port)
                                .setMeta(metadata));
    }
}
