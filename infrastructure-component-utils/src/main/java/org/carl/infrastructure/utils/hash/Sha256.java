package org.carl.infrastructure.utils.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public final class Sha256 {

    private Sha256() {
    }

    public static byte[] digest(byte[] value) {
        Objects.requireNonNull(value, "value must not be null");
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String hex(byte[] value) {
        return HexFormat.of().formatHex(digest(value));
    }

    public static String hex(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return hex(value.getBytes(StandardCharsets.UTF_8));
    }
}
