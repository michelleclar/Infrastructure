package org.carl.infrastructure.cache;

import io.smallrye.mutiny.Uni;
import java.util.List;
import org.carl.infrastructure.comment.Conversion;

interface BaseCache {
    Conversion get(String key);

    Uni<Void> del(String key);

    Uni<Boolean> put(String key, Object v);

    Uni<List<String>> keys();
}
