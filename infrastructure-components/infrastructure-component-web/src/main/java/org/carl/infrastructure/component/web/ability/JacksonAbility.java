package org.carl.infrastructure.component.web.ability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.carl.infrastructure.component.web.config.JacksonProvider;

public interface JacksonAbility {
    default String toJSONString(Object obj) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().writeValueAsString(obj);
    }

    default JsonNode toJsonObject(Object obj) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().convertValue(obj, JsonNode.class);
    }

    default <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().readValue(json, clazz);
    }

    default ObjectNode createObjectNode() {
        return JacksonProvider.JACKSON.get().createObjectNode();
    }

    default ObjectMapper getObjectMapper() {
        return JacksonProvider.JACKSON.get();
    }
}
