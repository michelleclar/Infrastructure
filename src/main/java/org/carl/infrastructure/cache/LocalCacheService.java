package org.carl.infrastructure.cache;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.carl.infrastructure.comment.Conversion;

@ApplicationScoped
public class LocalCacheService implements BaseCache {
    @CacheName("local")
    Cache cache;

    @Override
    public Conversion get(String key) {
        return this.executeValueCommands(
                o -> {
                    try {
                        return o.getIfPresent(key).get();
                    } catch (InterruptedException | ExecutionException e) {
                        return null;
                    }
                });
    }

    @Override
    public Uni<Void> del(String key) {
        return executeKeyCommands(o -> o.invalidate(key));
    }

    @Override
    public Uni<Boolean> put(String key, Object v) {
        return null;
    }

    @Override
    public Uni<List<String>> keys() {
        return executeKeyCommands(
                o -> Uni.createFrom().item(o.keySet().stream().map(Object::toString).toList()));
    }

    Conversion executeValueCommands(Function<CaffeineCache, Object> f) {
        return Conversion.create(f.apply(this.cache.as(CaffeineCache.class)));
    }

    <T> T executeKeyCommands(Function<CaffeineCache, T> f) {
        return f.apply(this.cache.as(CaffeineCache.class));
    }
}
