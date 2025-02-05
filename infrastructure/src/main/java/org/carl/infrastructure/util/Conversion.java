package org.carl.infrastructure.util;

import java.util.function.Function;
import org.carl.infrastructure.annotations.NotThreadSafe;
import org.carl.infrastructure.ability.GsonAbility;
import org.jboss.logging.Logger;

@NotThreadSafe
public class Conversion implements GsonAbility {
    private final Logger log = Logger.getLogger(Conversion.class);
    private final Object value;

    public static Conversion create(Object o) {
        return new Conversion(o);
    }

    Conversion(Object value) {
        this.value = value;
    }

    public <T> T to(Class<T> type) {
        return to(type::cast);
    }

    public Integer toInt() {
        return to(o -> Integer.parseInt(o.toString()));
    }

    public Integer toIntOrDefault(int defaultValue) {
        try {
            return toOrDefault(o -> Integer.parseInt(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public Long toLong() {
        return to(o -> Long.parseLong(o.toString()));
    }

    public Long toLongOrDefault(long defaultValue) {
        try {
            return toOrDefault(o -> Long.parseLong(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public Double toDouble() {
        return to(o -> Double.parseDouble(o.toString()));
    }

    public Double toDoubleOrDefault(double defaultValue) {
        try {
            return toOrDefault(o -> Double.parseDouble(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public Float toFloat() {
        return to(o -> Float.parseFloat(o.toString()));
    }

    public Float toFloatOrDefault(float defaultValue) {
        try {
            return toOrDefault(o -> Float.parseFloat(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public Boolean toBoolean() {
        return to(o -> Boolean.parseBoolean(o.toString()));
    }

    public Boolean toBooleanOrDefault(boolean defaultValue) {
        try {
            return toOrDefault(o -> Boolean.parseBoolean(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public Short toShort() {
        return to(o -> Short.parseShort(o.toString()));
    }

    public Short toShortOrDefault(short defaultValue) {
        try {
            return toOrDefault(o -> Short.parseShort(o.toString()), defaultValue);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }

    public String toJsonString() {
        return to(this::toJsonString);
    }

    public String toJsonObject() {
        return to(this::toJsonString);
    }

    public <T> T to(Function<Object, T> adapter) {
        return adapter.apply(this.value);
    }

    public <T> T toOrDefault(Function<Object, T> adapter, T defaultValue) {
        try {
            return adapter.apply(this.value);
        } catch (Exception e) {
            log.warnf("NonReactiveConversion failed, returning default value: %s", defaultValue);
            return defaultValue;
        }
    }
}
