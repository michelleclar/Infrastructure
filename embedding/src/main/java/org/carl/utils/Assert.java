package org.carl.utils;

import org.carl.infrastructure.component.web.config.exception.BizException;
import org.carl.infrastructure.utils.FieldsUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class Assert {
    public static void assertStringOrLongOrBoolean(Object value, String fieldName) {
        if (value == null) return;
        if (!FieldsUtils.isStringOrLongOrBoolean(value)) {
            throw BizException.biz(
                    "Field [%s] Only String, Long, Boolean are allowed.".formatted(fieldName));
        }
    }

    public static void assertAllStringOrLong(List<Object> list, String fieldName) {
        if (list == null || list.isEmpty()) return;
        if (list.stream().anyMatch(item -> !FieldsUtils.isStringOrLong(item))) {
            throw BizException.biz(
                    "Field [%s] only supports String and Long types.".formatted(fieldName));
        }
    }

    public static void assertTrue(Boolean flag, String errMessage) {
        if (flag) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertFalse(Boolean flag, String errMessage) {
        if (!flag) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertNull(Object object, String errMessage) {
        if (object != null) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertNotNull(Object object, String errMessage) {
        if (object == null) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertNotEmpty(String str, String errMessage) {
        if (str == null || str.isEmpty()) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertNotEmpty(Collection<?> collection, String errMessage) {
        if (collection == null || collection.isEmpty()) {
            throw BizException.biz(errMessage);
        }
    }

    public static void assertNotEmpty(Map<?, ?> map, String errMessage) {
        if (map == null || map.isEmpty()) {
            throw BizException.biz(errMessage);
        }
    }
}
