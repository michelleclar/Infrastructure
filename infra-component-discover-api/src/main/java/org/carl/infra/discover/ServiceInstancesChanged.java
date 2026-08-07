package org.carl.infra.discover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete service-instance-list transition. */
public record ServiceInstancesChanged(
        ServiceQuery query,
        List<ServiceInstance> previous,
        List<ServiceInstance> current) {

    public ServiceInstancesChanged {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        previous =
                Collections.unmodifiableList(
                        new ArrayList<>(previous == null ? List.of() : previous));
        current =
                Collections.unmodifiableList(
                        new ArrayList<>(current == null ? List.of() : current));
    }
}
