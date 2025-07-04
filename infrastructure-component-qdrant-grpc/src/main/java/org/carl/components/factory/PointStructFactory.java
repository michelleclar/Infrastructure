package org.carl.components.factory;

import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;

import org.jboss.logging.Logger;

import java.util.*;

public class PointStructFactory {
    private static final Logger logger = Logger.getLogger(PointStructFactory.class);

    public static Points.PointStruct buildPointStruct(UUID uuid, List<Float> vectors) {
        return buildPointStruct(PointIdFactory.id(uuid), VectorsFactory.vectors(vectors), null);
    }

    public static Points.PointStruct buildPointStruct(
            UUID uuid, Map<String, Points.Vector> vectors, Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> _payload = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    _payload.put(k, parseValue(v));
                });
        return buildPointStruct(
                PointIdFactory.id(uuid), VectorsFactory.namedVectors(vectors), _payload);
    }

    public static Points.PointStruct buildPointStruct(
            long id, Map<String, Points.Vector> vectors, Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> _payload = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    _payload.put(k, parseValue(v));
                });
        return buildPointStruct(
                PointIdFactory.id(id), VectorsFactory.namedVectors(vectors), _payload);
    }

    public static Points.PointStruct buildPointStruct(long id, List<Float> vectors) {
        return buildPointStruct(PointIdFactory.id(id), VectorsFactory.vectors(vectors), null);
    }

    public static Points.PointStruct buildPointStruct(long id, Map<String, List<Float>> vectors) {
        Map<String, Points.Vector> vector = new HashMap<>();
        vectors.forEach(
                (key, value) -> {
                    vector.put(key, VectorFactory.vector(value));
                });
        return buildPointStruct(PointIdFactory.id(id), VectorsFactory.namedVectors(vector), null);
    }

    public static Points.PointStruct buildPointStruct(
            UUID uuid, List<Float> vectors, Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> _payload = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    _payload.put(k, parseValue(v));
                });
        return buildPointStruct(PointIdFactory.id(uuid), VectorsFactory.vectors(vectors), _payload);
    }

    public static Points.PointStruct buildPointStruct(
            long id, List<Float> vectors, Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> _payload = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    _payload.put(k, parseValue(v));
                });
        return buildPointStruct(PointIdFactory.id(id), VectorsFactory.vectors(vectors), _payload);
    }

    public static Points.PointStruct buildPointStruct(
            Points.PointId id, List<Float> vectors, Map<String, Object> payload) {
        Map<String, JsonWithInt.Value> _payload = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    _payload.put(k, parseValue(v));
                });
        return buildPointStruct(id, VectorsFactory.vectors(vectors), _payload);
    }

    public static Points.PointStruct buildPointStruct(
            Points.PointId id, Points.Vectors vectors, Map<String, JsonWithInt.Value> payload) {
        Points.PointStruct.Builder builder = Points.PointStruct.newBuilder();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(vectors, "vectors");
        builder.setId(id);
        builder.setVectors(vectors);
        if (payload != null && !payload.isEmpty()) {
            builder.putAllPayload(payload);
        }
        return builder.build();
    }

    private static JsonWithInt.Value parseValue(Object v) {

        return switch (v) {
            case null -> ValueFactory.nullValue();
            case JsonWithInt.Value value -> value;
            case String s -> ValueFactory.value(s);
            case Integer i -> ValueFactory.value(i);
            case Long l -> ValueFactory.value(l);
            case Double d -> ValueFactory.value(d);
            case Boolean b -> ValueFactory.value(b);
            case List<?> l -> {
                List<JsonWithInt.Value> value = new ArrayList<>();
                for (Object o : l) value.add(parseValue(o));
                yield ValueFactory.value(value);
            }
            case Map<?, ?> m -> {
                Map<String, JsonWithInt.Value> value = new HashMap<>();
                m.forEach(
                        (k, _v) -> {
                            if (k instanceof String s) {
                                value.put(s, parseValue(_v));
                            } else {
                                logger.warnv(
                                        "map key only supports string, but got: {0}", k.getClass());
                            }
                        });
                yield ValueFactory.value(value);
            }
            default -> {
                logger.warnv("Unable to parse value of type {0}", v.getClass());
                yield ValueFactory.nullValue();
            }
        };
    }
}
