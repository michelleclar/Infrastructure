package org.carl.infrastructure.mq.source;

import java.util.Objects;

public record SourceInstanceReference(SourceReference source, int instanceId) {
    public SourceInstanceReference {
        Objects.requireNonNull(source, "source must not be null");
        if (instanceId < 0) {
            throw new IllegalArgumentException("instanceId must be greater than or equal to 0");
        }
    }

    public SourceInstanceReference(String tenant, String namespace, String name, int instanceId) {
        this(new SourceReference(tenant, namespace, name), instanceId);
    }
}
