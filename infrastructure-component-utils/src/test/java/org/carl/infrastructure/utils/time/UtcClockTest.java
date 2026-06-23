package org.carl.infrastructure.utils.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

class UtcClockTest {

    @Test
    void convertsInstantToUtcLocalDateTime() {
        assertEquals(
                LocalDateTime.of(2026, 6, 23, 0, 0),
                UtcClock.toUtcLocalDateTime(Instant.parse("2026-06-23T00:00:00Z")));
        assertEquals(ZoneOffset.UTC, UtcClock.clock().getZone());
    }
}
