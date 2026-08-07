package org.carl.infra.discover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

class DynamicConfigSnapshotTest {

    @Test
    void convertsSupportedTypesAndCopiesValues() {
        Map<String, String> values = new HashMap<>();
        values.put("enabled", "true");
        values.put("limit", "12");
        values.put("timeout", "PT5S");

        DynamicConfigSnapshot snapshot =
                new DynamicConfigSnapshot("7", Instant.parse("2026-01-01T00:00:00Z"), values);
        values.put("limit", "99");

        assertEquals(true, snapshot.get("enabled", boolean.class));
        assertEquals(12, snapshot.get("limit", int.class));
        assertEquals(Duration.ofSeconds(5), snapshot.get("timeout", Duration.class));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.values().put("other", "value"));
    }

    @Test
    void rejectsInvalidBooleanAndUnsupportedTypes() {
        DynamicConfigSnapshot snapshot =
                new DynamicConfigSnapshot(
                        "8", Instant.parse("2026-01-01T00:00:00Z"), Map.of("enabled", "yes"));

        assertThrows(
                IllegalArgumentException.class, () -> snapshot.get("enabled", Boolean.class));
        assertThrows(
                IllegalArgumentException.class,
                () -> snapshot.get("enabled", DynamicConfigSnapshot.class));
    }
}
