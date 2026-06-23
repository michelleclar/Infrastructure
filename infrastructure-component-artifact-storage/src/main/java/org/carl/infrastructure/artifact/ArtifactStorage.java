package org.carl.infrastructure.artifact;

import java.io.IOException;
import java.util.Optional;

public interface ArtifactStorage {

    ArtifactMetadata save(ArtifactWriteRequest request) throws IOException;

    Optional<ArtifactMetadata> findMetadata(String key) throws IOException;

    byte[] read(String key) throws IOException;
}
