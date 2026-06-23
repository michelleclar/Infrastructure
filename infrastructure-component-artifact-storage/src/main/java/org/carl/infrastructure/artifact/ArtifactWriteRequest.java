package org.carl.infrastructure.artifact;

import java.util.Arrays;
import java.util.Objects;

public record ArtifactWriteRequest(String key, byte[] content, String contentType) {

    public ArtifactWriteRequest {
        key = ArtifactKeys.requireValid(key);
        content = Arrays.copyOf(Objects.requireNonNull(content, "content must not be null"), content.length);
        contentType = normalizeContentType(contentType);
    }

    public static ArtifactWriteRequest of(String key, byte[] content, String contentType) {
        return new ArtifactWriteRequest(key, content, contentType);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    private static String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "application/octet-stream";
        }
        return value.trim();
    }
}
