package org.carl.infrastructure.cache;

import io.smallrye.mutiny.Uni;
import java.util.List;

interface BaseCache {
    Uni<Object> get(String key);

    Uni<Void> del(String key);

    Uni<Void> set(String key, Object v);

    Uni<List<String>> keys();
}
