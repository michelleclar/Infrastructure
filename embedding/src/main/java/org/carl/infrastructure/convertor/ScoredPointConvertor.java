package org.carl.infrastructure.convertor;

import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;

import org.carl.client.dto.clientobject.ScoredPointCO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoredPointConvertor {
    public static ScoredPointCO toClientObject(Points.ScoredPoint scoredPoint) {
        return from(scoredPoint);
    }

    private static ScoredPointCO from(Points.ScoredPoint point) {
        ScoredPointCO dto = new ScoredPointCO();

        if (point.hasId()) {
            if (point.getId().hasNum()) {
                dto.id = point.getId().getNum();
            }
            if (point.getId().hasUuid()) {
                dto.uuid = point.getId().getUuid();
            }
        }

        dto.score = point.getScore();
        dto.payload = protobufStructToMap(point.getPayloadMap());
        return dto;
    }

    private static Map<String, Object> protobufStructToMap(Map<String, JsonWithInt.Value> payload) {
        if (payload.isEmpty()) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        payload.forEach(
                (k, v) -> {
                    map.put(k, extractValue(v));
                });
        return map;
    }

    private static Map<String, Object> extractValue(JsonWithInt.Struct struct) {
        return protobufStructToMap(struct.getFieldsMap());
    }

    private static Object extractValue(JsonWithInt.Value value) {
        switch (value.getKindCase()) {
            case STRING_VALUE:
                return value.getStringValue();
            case INTEGER_VALUE:
                return value.getIntegerValue();
            case BOOL_VALUE:
                return value.getBoolValue();
            case DOUBLE_VALUE:
                return value.getDoubleValue();
            case STRUCT_VALUE:
                return extractValue(value.getStructValue());
            case LIST_VALUE:
                List<Object> list = new ArrayList<>();
                for (JsonWithInt.Value v : value.getListValue().getValuesList()) {
                    list.add(extractValue(v));
                }
                return list;
            default:
                return null;
        }
    }
}
