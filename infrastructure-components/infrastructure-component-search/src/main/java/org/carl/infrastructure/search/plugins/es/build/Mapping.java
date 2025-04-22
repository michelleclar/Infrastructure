package org.carl.infrastructure.search.plugins.es.build;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map.Entry;
import java.util.function.Function;

public class Mapping {
    ObjectNode json;
    String rootName = "properties";

    public static Mapping build() {
        return new Mapping();
    }

    Properties properties() {
        return new Properties(this);
    }

    public Mapping properties(Function<Properties, Mapping> f) {
        return f.apply(properties());
    }

    Mapping addProperties(ObjectNode jsonNode) {
        if (json == null) json = new ObjectMapper().createObjectNode();
        JsonNode jsonNode1 = json.get(rootName);
        if (jsonNode1 != null) {
            for (Entry<String, JsonNode> property : jsonNode1.properties()) {
                jsonNode.set(property.getKey(), property.getValue());
            }
        }
        json.set(rootName, jsonNode);
        return this;
    }

    @Override
    public String toString() {
        return json.toString();
    }

    public class Properties {
        Mapping mapping;
        String name;
        PropertyType type;

        public Properties(Mapping mapping) {
            this.mapping = mapping;
        }

        public Properties setType(PropertyType type) {
            this.type = type;
            return this;
        }

        public Properties setName(String name) {
            this.name = name;
            return this;
        }

        public Mapping build() {
            ObjectNode jsonNode = new ObjectMapper().createObjectNode();
            ObjectNode inner = new ObjectMapper().createObjectNode();
            inner.put("type", type.name().toLowerCase());
            jsonNode.set(name, inner);
            return mapping.addProperties(jsonNode);
        }
    }
}
