package org.carl.infrastructure.cache;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class ReactiveLocalCacheService implements BaseCache {
    @CacheName("local")
    Cache cache;

    @Override
    public Uni<Object> get(String key) {
        return this.executeValueCommands(o -> cache.get(key, a -> a));
    }

    @Override
    public Uni<Void> del(String key) {
        return executeKeyCommands(o -> o.invalidate(key));
    }

    @Override
    public Uni<Void> set(String key, Object v) {
        return null;
    }

    @Override
    public Uni<List<String>> keys() {
        return executeKeyCommands(
                o ->
                        Uni.createFrom()
                                .item(
                                        o.as(CaffeineCache.class).keySet().stream()
                                                .map(Object::toString)
                                                .toList()));
    }

    <T> T executeValueCommands(Function<Cache, T> f) {
        return f.apply(this.cache);
    }

    <T> T executeKeyCommands(Function<Cache, T> f) {
        return f.apply(this.cache);
    }
}
