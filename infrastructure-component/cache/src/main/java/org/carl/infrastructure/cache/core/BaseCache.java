package org.carl.infrastructure.cache.core;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface BaseCache {
    Uni<Object> get(String key);

    Uni<Void> del(String key);

    Uni<Void> set(String key, Object v);

    Uni<List<String>> keys();
}
