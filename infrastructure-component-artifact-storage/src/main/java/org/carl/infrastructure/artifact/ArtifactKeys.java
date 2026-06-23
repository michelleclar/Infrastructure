package org.carl.infrastructure.artifact;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

public final class ArtifactKeys {

    private ArtifactKeys() {
    }

    public static String requireValid(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String normalized = key.replace('\\', '/').trim();
        if (normalized.isEmpty()) {
            throw new ArtifactStorageException("key must not be blank");
        }
        if (normalized.startsWith("/") || normalized.contains("//")) {
            throw new ArtifactStorageException("key must be a relative normalized path");
        }
        try {
            Path path = Path.of(normalized);
            if (path.isAbsolute()) {
                throw new ArtifactStorageException("key must be relative");
            }
            for (Path part : path) {
                String value = part.toString();
                if (".".equals(value) || "..".equals(value)) {
                    throw new ArtifactStorageException("key must not contain traversal segments");
                }
            }
            return normalized;
        } catch (InvalidPathException e) {
            throw new ArtifactStorageException("key is not a valid path", e);
        }
    }
}
