package org.carl.infra.cache;

import io.smallrye.mutiny.Uni;
import java.util.function.Function;

public interface ICacheOperations {
    <T> T get(String cacheName, String key, Function<String, T> valueLoader);
    void invalidate(String cacheName, String key);
}
