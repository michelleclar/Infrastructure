package org.carl.infrastructure.artifact.local;

import org.carl.infrastructure.artifact.ArtifactKeys;
import org.carl.infrastructure.artifact.ArtifactMetadata;
import org.carl.infrastructure.artifact.ArtifactStorage;
import org.carl.infrastructure.artifact.ArtifactStorageException;
import org.carl.infrastructure.artifact.ArtifactWriteRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class LocalArtifactStorage implements ArtifactStorage {

    private static final String METADATA_SUFFIX = ".metadata.properties";

    private final Path root;
    private final Clock clock;

    public LocalArtifactStorage(Path root) {
        this(root, Clock.systemUTC());
    }

    public LocalArtifactStorage(Path root, Clock clock) {
        this.root = Objects.requireNonNull(root, "root must not be null").toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ArtifactMetadata save(ArtifactWriteRequest request) throws IOException {
        Path dataPath = resolveDataPath(request.key());
        Files.createDirectories(dataPath.getParent());
        Files.write(dataPath, request.content());

        ArtifactMetadata metadata =
                new ArtifactMetadata(
                        request.key(),
                        request.contentType(),
                        request.content().length,
                        root.relativize(dataPath).toString(),
                        Instant.now(clock));
        writeMetadata(dataPath, metadata);
        return metadata;
    }

    @Override
    public Optional<ArtifactMetadata> findMetadata(String key) throws IOException {
        Path dataPath = resolveDataPath(key);
        Path metadataPath = metadataPath(dataPath);
        if (!Files.exists(metadataPath)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(metadataPath)) {
            properties.load(input);
        }
        return Optional.of(readMetadata(properties));
    }

    @Override
    public byte[] read(String key) throws IOException {
        Path dataPath = resolveDataPath(key);
        try {
            return Files.readAllBytes(dataPath);
        } catch (NoSuchFileException e) {
            throw new ArtifactStorageException("artifact not found: " + key, e);
        }
    }

    private Path resolveDataPath(String key) {
        String validKey = ArtifactKeys.requireValid(key);
        Path resolved = root.resolve(validKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new ArtifactStorageException("artifact key resolves outside storage root");
        }
        return resolved;
    }

    private Path metadataPath(Path dataPath) {
        return dataPath.resolveSibling(dataPath.getFileName() + METADATA_SUFFIX);
    }

    private void writeMetadata(Path dataPath, ArtifactMetadata metadata) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("key", metadata.key());
        properties.setProperty("contentType", metadata.contentType());
        properties.setProperty("byteSize", Long.toString(metadata.byteSize()));
        properties.setProperty("storagePath", metadata.storagePath());
        properties.setProperty("createdAt", metadata.createdAt().toString());

        try (OutputStream output = Files.newOutputStream(metadataPath(dataPath))) {
            properties.store(output, "artifact metadata");
        }
    }

    private ArtifactMetadata readMetadata(Properties properties) {
        return new ArtifactMetadata(
                properties.getProperty("key"),
                properties.getProperty("contentType"),
                Long.parseLong(properties.getProperty("byteSize")),
                properties.getProperty("storagePath"),
                Instant.parse(properties.getProperty("createdAt")));
    }
}
