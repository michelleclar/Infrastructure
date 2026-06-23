package org.carl.infrastructure.artifact.local;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.carl.infrastructure.artifact.ArtifactMetadata;
import org.carl.infrastructure.artifact.ArtifactStorage;
import org.carl.infrastructure.artifact.ArtifactStorageException;
import org.carl.infrastructure.artifact.ArtifactWriteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class LocalArtifactStorageTest {

    @TempDir Path tempDir;

    @Test
    void savesContentAndMetadata() throws Exception {
        ArtifactStorage storage =
                new LocalArtifactStorage(
                        tempDir, Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC));

        ArtifactMetadata metadata =
                storage.save(
                        ArtifactWriteRequest.of(
                                "audit/2026/report.json",
                                "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                                "application/json"));

        assertEquals("audit/2026/report.json", metadata.key());
        assertEquals("application/json", metadata.contentType());
        assertEquals(11, metadata.byteSize());
        assertTrue(Files.exists(tempDir.resolve("audit/2026/report.json")));
        assertTrue(Files.exists(tempDir.resolve("audit/2026/report.json.metadata.properties")));
        assertArrayEquals(
                "{\"ok\":true}".getBytes(StandardCharsets.UTF_8),
                storage.read("audit/2026/report.json"));
        assertEquals(metadata, storage.findMetadata("audit/2026/report.json").orElseThrow());
    }

    @Test
    void rejectsPathTraversal() {
        ArtifactStorage storage = new LocalArtifactStorage(tempDir);

        assertThrows(
                ArtifactStorageException.class,
                () -> storage.save(ArtifactWriteRequest.of("../secret.txt", new byte[] {1}, "text/plain")));
        assertThrows(
                ArtifactStorageException.class,
                () -> storage.read("/absolute/secret.txt"));
    }

    @Test
    void createsParentDirectoriesSafely() throws Exception {
        ArtifactStorage storage = new LocalArtifactStorage(tempDir);

        storage.save(ArtifactWriteRequest.of("a/b/c.txt", "hello".getBytes(StandardCharsets.UTF_8), "text/plain"));

        assertTrue(Files.isDirectory(tempDir.resolve("a/b")));
        assertEquals("hello", Files.readString(tempDir.resolve("a/b/c.txt")));
    }

    @Test
    void exposesProviderAbstraction() throws Exception {
        ArtifactStorage storage = new LocalArtifactStorage(tempDir);

        storage.save(ArtifactWriteRequest.of("x.bin", new byte[] {1, 2, 3}, null));

        assertEquals("application/octet-stream", storage.findMetadata("x.bin").orElseThrow().contentType());
        assertArrayEquals(new byte[] {1, 2, 3}, storage.read("x.bin"));
    }
}
