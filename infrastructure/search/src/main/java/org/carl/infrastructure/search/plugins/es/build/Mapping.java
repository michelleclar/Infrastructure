package org.carl.infrastructure.search.plugins.es.build;

import static org.carl.infrastructure.util.JSON.JSON;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map.Entry;

public class Mapping {
    ObjectNode json;
    String rootName = "properties";

    public static Mapping build() {
        return new Mapping();
    }

    public Properties properties() {
        return new Properties(this);
    }

    Mapping addProperties(ObjectNode jsonNode) {
        if (json == null) json = JSON.createObjectNode();
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
}

class Properties {
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
        ObjectNode jsonNode = JSON.createObjectNode();
        ObjectNode inner = JSON.createObjectNode();
        inner.put("type", type.name().toLowerCase());
        jsonNode.set(name, inner);
        return mapping.addProperties(jsonNode);
    }
}
