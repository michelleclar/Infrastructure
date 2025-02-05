package org.carl.infrastructure.util;

@FunctionalInterface
public interface GetFunction<R> {
    R apply();
}
