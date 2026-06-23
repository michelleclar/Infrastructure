package org.carl.infrastructure.mq.source;

import java.util.List;

public record SourceStatus(int numInstances, int numRunning, List<SourceInstanceStatus> instances) {
    public SourceStatus {
        instances = instances == null ? List.of() : List.copyOf(instances);
    }
}
