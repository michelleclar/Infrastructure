package org.carl.infra.redis.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class JacksonRedisValueCodecTest {

    private final RedisValueCodec codec = new JacksonRedisValueCodec();

    @Test
    void roundTripsJavaTimeValue() {
        Value value = new Value("id-1", LocalDateTime.of(2026, 7, 14, 12, 30));

        String encoded = codec.encode(value);
        Value decoded = codec.decode(encoded, Value.class);

        assertEquals(value, decoded);
    }

    @Test
    void decodesGenericValue() {
        String encoded = codec.encode(List.of("a", "b"));

        List<String> decoded = codec.decode(encoded, new TypeReference<List<String>>() {});

        assertEquals(List.of("a", "b"), decoded);
    }

    record Value(String id, LocalDateTime createdAt) {}
}
