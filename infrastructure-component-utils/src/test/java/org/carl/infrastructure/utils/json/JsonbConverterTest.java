package org.carl.infrastructure.utils.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jooq.JSONB;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class JsonbConverterTest {

    record Nested(String value) {}

    record Payload(String name, LocalDateTime createdAt, List<Nested> nested) {}

    @Test
    void roundTripsJsonbWithJavaTime() {
        JsonbConverter converter = new JsonbConverter();
        Payload payload =
                new Payload(
                        "demo",
                        LocalDateTime.of(2026, 6, 23, 12, 30),
                        List.of(new Nested("a")));

        JSONB jsonb = converter.toJsonb(payload);
        Payload restored = converter.fromJsonb(jsonb, Payload.class);

        assertEquals(payload, restored);
    }
}
