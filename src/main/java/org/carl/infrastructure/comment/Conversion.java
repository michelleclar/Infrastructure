package org.carl.infrastructure.comment;

import io.smallrye.mutiny.Uni;
import java.util.function.Function;
import org.carl.infrastructure.annotations.NotThreadSafe;
import org.carl.infrastructure.comment.parse.json.JSON;
import org.jboss.logging.Logger;

@NotThreadSafe
public class Conversion {
    private final Logger log = Logger.getLogger(Conversion.class);
    private final Object value;

    public static Conversion create(Object o) {
        return new Conversion(o);
    }

    Conversion(Object value) {
        this.value = value;
    }

    public <T> Uni<T> to(Class<T> type) {
        return this.to(type::cast);
    }

    public Uni<Integer> toInt() {
        return this.to(o -> Integer.parseInt(o.toString()));
    }

    public Uni<Integer> toIntOrDefault(int defaultValue) {
        return this.toOrDefault(o -> Integer.parseInt(o.toString()), defaultValue);
    }

    public Uni<Long> toLong() {
        return this.to(o -> Long.parseLong(o.toString()));
    }

    public Uni<Long> toLongOrDefault(long defaultValue) {
        return this.toOrDefault(o -> Long.parseLong(o.toString()), defaultValue);
    }

    public Uni<Double> toDouble() {
        return this.to(o -> Double.parseDouble(o.toString()));
    }

    public Uni<Double> toDoubleOrDefault(double defaultValue) {
        return this.toOrDefault(o -> Double.parseDouble(o.toString()), defaultValue);
    }

    public Uni<Float> toFloat() {
        return this.to(o -> Float.parseFloat(o.toString()));
    }

    public Uni<Float> toFloatOrDefault(float defaultValue) {
        return this.toOrDefault(o -> Float.parseFloat(o.toString()), defaultValue);
    }

    public Uni<Boolean> toBoolean() {
        return this.to(o -> Boolean.parseBoolean(o.toString()));
    }

    public Uni<Boolean> toBooleanOrDefault(boolean defaultValue) {
        return this.toOrDefault(o -> Boolean.parseBoolean(o.toString()), defaultValue);
    }

    public Uni<Short> toShort() {
        return this.to(o -> Short.parseShort(o.toString()));
    }

    public Uni<Short> toShortOrDefault(short defaultValue) {
        return this.toOrDefault(o -> Short.parseShort(o.toString()), defaultValue);
    }

    public Uni<String> toJsonString() {
        return this.to(o -> JSON.toJsonString(this.value));
    }

    public Uni<String> toJsonObject() {
        return this.to(o -> JSON.toJsonString(this.value));
    }

    public <T> Uni<T> to(Function<Object, T> adapter) {
        return Uni.createFrom().item(adapter.apply(this.value));
    }

    public <T> Uni<T> toOrDefault(Function<Object, T> adapter, T defaultValue) {
        return Uni.createFrom()
                .item(
                        () -> {
                            try {
                                return adapter.apply(this.value);
                            } catch (Exception e) {
                                log.warnf(
                                        "Conversion failed, returning default value: %s"
                                                + defaultValue);
                                return defaultValue;
                            }
                        });
    }

    public Boolean isNull() {
        return this.value == null;
    }
}
