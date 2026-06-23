package org.carl.infrastructure.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jooq.JSONB;

import java.util.Objects;

public class JsonbConverter {

    private final ObjectMapper mapper;

    public JsonbConverter() {
        this(SharedObjectMapper.create());
    }

    public JsonbConverter(ObjectMapper mapper) {
        this.mapper = SharedObjectMapper.copyOf(mapper);
    }

    public JSONB toJsonb(Object value) {
        try {
            return JSONB.jsonb(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSONB serialization failed", e);
        }
    }

    public <T> T fromJsonb(JSONB jsonb, Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        try {
            return mapper.readValue(requireData(jsonb), type);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSONB deserialization failed", e);
        }
    }

    public <T> T fromJsonb(JSONB jsonb, TypeReference<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        try {
            return mapper.readValue(requireData(jsonb), type);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSONB deserialization failed", e);
        }
    }

    private String requireData(JSONB jsonb) {
        Objects.requireNonNull(jsonb, "jsonb must not be null");
        return jsonb.data();
    }
}
