package org.carl.infrastructure.utils.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class UtcClock {

    private static final Clock UTC = Clock.systemUTC();

    private UtcClock() {
    }

    public static Clock clock() {
        return UTC;
    }

    public static Instant now() {
        return Instant.now(UTC);
    }

    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.ofInstant(now(), ZoneOffset.UTC);
    }

    public static LocalDateTime toUtcLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
