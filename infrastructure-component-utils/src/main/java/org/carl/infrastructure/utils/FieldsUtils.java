package org.carl.infrastructure.utils;

import java.util.List;

public class FieldsUtils {
    public static boolean isString(Object value) {
        return value instanceof String;
    }

    public static boolean isInteger(Object value) {
        return value instanceof Integer;
    }

    public static boolean isLong(Object value) {
        return value instanceof Long || isInteger(value);
    }

    public static boolean isFloat(Object value) {
        return value instanceof Float;
    }

    public static boolean isDouble(Object value) {
        return value instanceof Double;
    }

    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    public static boolean isStringOrLongOrBoolean(Object value) {
        return isString(value) || isLong(value) || isBoolean(value);
    }

    public static boolean isStringOrLong(Object value) {
        return isString(value) || isLong(value);
    }

    public static boolean isNumber(Object value) {
        return value instanceof Number;
    }

    public static boolean isList(Object value) {
        return value instanceof List;
    }

    public static boolean isPrimitiveType(Object value) {
        return isString(value)
                || isInteger(value)
                || isLong(value)
                || isFloat(value)
                || isDouble(value)
                || isBoolean(value);
    }

    public static String typeOf(Object value) {
        if (value == null) return "null";
        return value.getClass().getSimpleName();
    }
}
