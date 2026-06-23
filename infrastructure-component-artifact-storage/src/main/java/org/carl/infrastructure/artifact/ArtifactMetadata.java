package org.carl.infrastructure.artifact;

import java.time.Instant;
import java.util.Objects;

public record ArtifactMetadata(
        String key,
        String contentType,
        long byteSize,
        String storagePath,
        Instant createdAt) {

    public ArtifactMetadata {
        key = ArtifactKeys.requireValid(key);
        contentType = Objects.requireNonNull(contentType, "contentType must not be null");
        if (byteSize < 0) {
            throw new IllegalArgumentException("byteSize must not be negative");
        }
        storagePath = Objects.requireNonNull(storagePath, "storagePath must not be null");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
