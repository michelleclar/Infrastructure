package org.carl.infrastructure.redis.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Objects;

public final class JacksonRedisValueCodec implements RedisValueCodec {

    private final ObjectMapper objectMapper;

    public JacksonRedisValueCodec() {
        this(List.of());
    }

    public JacksonRedisValueCodec(List<Module> modules) {
        this(createObjectMapper(modules));
    }

    public JacksonRedisValueCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    private static ObjectMapper createObjectMapper(List<Module> modules) {
        ObjectMapper mapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        modules.forEach(mapper::registerModule);
        return mapper;
    }

    @Override
    public String encode(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode redis value", e);
        }
    }

    @Override
    public <T> T decode(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to decode redis value", e);
        }
    }

    @Override
    public <T> T decode(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to decode redis value", e);
        }
    }
}
