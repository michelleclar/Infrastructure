package org.carl.infrastructure.util;

import io.smallrye.mutiny.Uni;

import java.util.function.Function;

import org.carl.infrastructure.annotations.NotThreadSafe;
import org.carl.infrastructure.core.ability.GsonAbility;
import org.jboss.logging.Logger;

@NotThreadSafe
public class ReactiveConversion implements GsonAbility {
    private final Logger log = Logger.getLogger(ReactiveConversion.class);
    private final Object value;

    public static ReactiveConversion create(Object o) {
        return new ReactiveConversion(o);
    }

    ReactiveConversion(Object value) {
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
        return this.to(this::toJsonString);
    }

    public Uni<String> toJsonObject() {
        return this.to(this::toJsonString);
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
                                        "ReactiveConversion failed, returning default value: %s"
                                                + defaultValue);
                                return defaultValue;
                            }
                        });
    }
}
