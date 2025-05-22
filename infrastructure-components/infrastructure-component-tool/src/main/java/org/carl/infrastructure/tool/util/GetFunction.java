package org.carl.infrastructure.tool.util;

@FunctionalInterface
public interface GetFunction<R> {
    R apply();
}
