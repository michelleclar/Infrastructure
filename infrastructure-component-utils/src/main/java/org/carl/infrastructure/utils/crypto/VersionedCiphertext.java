package org.carl.infrastructure.utils.crypto;

import java.util.Objects;

public record VersionedCiphertext(String version, String iv, String payload) {

    public VersionedCiphertext {
        version = requireText(version, "version");
        iv = requireText(iv, "iv");
        payload = requireText(payload, "payload");
    }

    public static VersionedCiphertext parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        String[] parts = value.split(":", -1);
        if (parts.length != 3) {
            throw new CryptoException("Ciphertext must use version:iv:payload format");
        }
        return new VersionedCiphertext(parts[0], parts[1], parts[2]);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new CryptoException(name + " must not be blank");
        }
        return value;
    }
}
