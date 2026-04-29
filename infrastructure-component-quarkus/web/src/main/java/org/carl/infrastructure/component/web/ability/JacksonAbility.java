package org.carl.infrastructure.component.web.ability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.carl.infrastructure.component.web.config.JacksonProvider;
import org.carl.infrastructure.component.web.config.exception.BizException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface JacksonAbility {
    default String toJsonString(Object obj) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().writeValueAsString(obj);
    }

    default String toJsonStringX(Object obj) {
        try {
            return JacksonProvider.JACKSON.get().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw BizException.biz(e.getMessage());
        }
    }

    default JsonNode toJsonNode(String json) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().readTree(json);
    }

    default JsonNode toJsonNodeX(String json) {
        try {
            return JacksonProvider.JACKSON.get().readTree(json);
        } catch (JsonProcessingException e) {
            throw BizException.biz(e.getMessage());
        }
    }


    default ObjectNode toJsonObjectX(Object obj) {
        return this.convertValue(obj, ObjectNode.class);
    }

    default <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().readValue(json, clazz);
    }

    default <T> T fromJsonX(String json, Class<T> clazz) {
        try {
            return JacksonProvider.JACKSON.get().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw BizException.biz(e.getMessage());
        }
    }

    default <T> T fromJson(String json, TypeReference<T> t) throws JsonProcessingException {
        return JacksonProvider.JACKSON.get().readValue(json, t);
    }
    default ObjectNode mapToJsonNode(Map<String, Object> map) {
        return this.convertValue(map, ObjectNode.class);
    }

    default <T> T convertValue(Object obj, Class<T> clazz) {
        return JacksonProvider.JACKSON.get().convertValue(obj, clazz);
    }

    default <T> T convertValue(Object obj, TypeReference<T> t) {
        return JacksonProvider.JACKSON.get().convertValue(obj, t);
    }

    default ObjectNode createObjectNode() {
        return JacksonProvider.JACKSON.get().createObjectNode();
    }

    default ObjectMapper getObjectMapper() {
        return JacksonProvider.JACKSON.get();
    }

    default ObjectNode merge(ObjectNode source, JsonNode target) {
        ObjectNode r = source.deepCopy();
        if (target != null) {
            for (Map.Entry<String, JsonNode> property : target.properties()) {
                r.set(property.getKey(), property.getValue());
            }
        }
        return r;
    }

    default JsonNode mergeJsonNodes(JsonNode node1, JsonNode node2) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        Set<String> allFieldNames = new HashSet<>();
        node1.fieldNames().forEachRemaining(allFieldNames::add);
        node2.fieldNames().forEachRemaining(allFieldNames::add);

        for (String fieldName : allFieldNames) {
            JsonNode value1 = node1.get(fieldName);
            JsonNode value2 = node2.get(fieldName);

            ArrayNode arrayNode = mapper.createArrayNode();
            if (value1 != null) {
                arrayNode.add(value1);
            }
            if (value2 != null) {
                arrayNode.add(value2);
            }

            result.set(fieldName, arrayNode);
        }

        return result;
    }

    default Map<String,Object> toMap(Object obj) {
        return convertValue(obj, new TypeReference<>() {});
    }
}


