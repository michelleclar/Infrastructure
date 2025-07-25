package org.carl.infrastructure.qdrant.factory;

import io.qdrant.client.grpc.JsonWithInt.ListValue;
import io.qdrant.client.grpc.JsonWithInt.NullValue;
import io.qdrant.client.grpc.JsonWithInt.Struct;
import io.qdrant.client.grpc.JsonWithInt.Value;
import java.util.List;
import java.util.Map;

/** Convenience methods for constructing {@link Value} */
public final class ValueFactory {
    private ValueFactory() {}

    /**
     * Creates a value from a {@link String}
     *
     * @param value The value
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(String value) {
        return Value.newBuilder().setStringValue(value).build();
    }

    /**
     * Creates a value from a {@link long}
     *
     * @param value The value
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(long value) {
        return Value.newBuilder().setIntegerValue(value).build();
    }

    /**
     * Creates a value from a {@link double}
     *
     * @param value The value
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(double value) {
        return Value.newBuilder().setDoubleValue(value).build();
    }

    /**
     * Creates a value from a {@link boolean}
     *
     * @param value The value
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(boolean value) {
        return Value.newBuilder().setBoolValue(value).build();
    }

    /**
     * Creates a null value
     *
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value nullValue() {
        return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
    }

    /**
     * Creates a value from a list of values
     *
     * @param values The list of values
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value list(List<Value> values) {
        return Value.newBuilder()
                .setListValue(ListValue.newBuilder().addAllValues(values).build())
                .build();
    }

    /**
     * Creates a value from a list of values. Same as {@link #list(List)}
     *
     * @param values The list of values
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(List<Value> values) {
        return list(values);
    }

    /**
     * Creates a value from a map of nested values
     *
     * @param values The map of values
     * @return a new instance of {@link io.qdrant.client.grpc.JsonWithInt.Value}
     */
    public static Value value(Map<String, Value> values) {
        return Value.newBuilder()
                .setStructValue(Struct.newBuilder().putAllFields(values).build())
                .build();
    }
}
