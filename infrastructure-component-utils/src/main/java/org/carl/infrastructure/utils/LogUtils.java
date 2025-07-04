package org.carl.infrastructure.utils;

public class LogUtils {
    public static String toShortString(Object o, int maxLength) {
        if (o == null) {
            return "null";
        }
        String str = o.toString();
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
