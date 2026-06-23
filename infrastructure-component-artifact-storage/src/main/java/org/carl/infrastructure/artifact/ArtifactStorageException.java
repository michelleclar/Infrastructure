package org.carl.infrastructure.artifact;

import java.io.Serial;

public class ArtifactStorageException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    public ArtifactStorageException(String message) {
        super(message);
    }

    public ArtifactStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
