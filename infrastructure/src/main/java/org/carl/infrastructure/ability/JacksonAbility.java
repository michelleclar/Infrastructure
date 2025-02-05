package org.carl.infrastructure.ability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.carl.infrastructure.parse.json.JacksonProvider;

public interface JacksonAbility {
    default String toJsonString(Object obj) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().writeValueAsString(obj);
    }

    default JsonNode toJsonObject(Object obj) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().convertValue(obj, JsonNode.class);
    }

    default <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().readValue(json, clazz);
    }

    default ObjectMapper getObjectMapper() {
        return JacksonProvider.JACKSON.get();
    }
}
