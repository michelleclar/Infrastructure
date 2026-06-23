package org.carl.infrastructure.mq.source;

public record SourceReference(String tenant, String namespace, String name) {
    public SourceReference {
        requireText(tenant, "tenant");
        requireText(namespace, "namespace");
        requireText(name, "name");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
