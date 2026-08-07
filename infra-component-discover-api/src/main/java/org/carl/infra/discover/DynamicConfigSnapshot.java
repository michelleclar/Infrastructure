package org.carl.infra.discover;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;

/** Immutable, atomically activated configuration document. */
public record DynamicConfigSnapshot(
        String version, Instant loadedAt, Map<String, String> values) {

    public DynamicConfigSnapshot {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        if (loadedAt == null) {
            throw new IllegalArgumentException("loadedAt must not be null");
        }
        values =
                Collections.unmodifiableMap(
                        new TreeMap<>(values == null ? Map.of() : values));
    }

    public static DynamicConfigSnapshot empty() {
        return new DynamicConfigSnapshot("0", Instant.EPOCH, Map.of());
    }

    public <T> T get(String key, Class<T> type) {
        return getOptional(key, type)
                .orElseThrow(() -> new NoSuchElementException("Missing configuration key: " + key));
    }

    public <T> Optional<T> getOptional(String key, Class<T> type) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        String value = values.get(key);
        return value == null ? Optional.empty() : Optional.of(convert(key, value, type));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T convert(String key, String value, Class<T> type) {
        try {
            Object converted;
            if (type == String.class) {
                converted = value;
            } else if (type == Boolean.class || type == boolean.class) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new IllegalArgumentException("expected true or false");
                }
                converted = Boolean.valueOf(value);
            } else if (type == Byte.class || type == byte.class) {
                converted = Byte.valueOf(value);
            } else if (type == Short.class || type == short.class) {
                converted = Short.valueOf(value);
            } else if (type == Integer.class || type == int.class) {
                converted = Integer.valueOf(value);
            } else if (type == Long.class || type == long.class) {
                converted = Long.valueOf(value);
            } else if (type == Float.class || type == float.class) {
                converted = Float.valueOf(value);
            } else if (type == Double.class || type == double.class) {
                converted = Double.valueOf(value);
            } else if (type == Character.class || type == char.class) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException("expected one character");
                }
                converted = value.charAt(0);
            } else if (type == BigInteger.class) {
                converted = new BigInteger(value);
            } else if (type == BigDecimal.class) {
                converted = new BigDecimal(value);
            } else if (type == Duration.class) {
                converted = Duration.parse(value);
            } else if (type == URI.class) {
                converted = URI.create(value);
            } else if (type.isEnum()) {
                converted = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), value);
            } else {
                throw new IllegalArgumentException("unsupported target type " + type.getName());
            }
            return (T) converted;
        } catch (RuntimeException error) {
            throw new IllegalArgumentException(
                    "Invalid value for configuration key '" + key + "' as " + type.getName(),
                    error);
        }
    }
}
