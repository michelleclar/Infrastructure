package org.carl.infra.cache;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.cache.CacheManager;
import io.smallrye.mutiny.Uni;
import java.util.function.Function;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.plugins.cache.enable", stringValue = "true")
public class CacheService extends CacheStd implements ICacheOperations {

    @Inject
    CacheManager cacheManager;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Function<String, T> valueLoader) {
        return (T) cacheManager.getCache(cacheName)
                .orElseThrow(() -> new IllegalArgumentException("Cache not found: " + cacheName))
                .get(key, k -> Uni.createFrom().item(() -> valueLoader.apply((String) k)))
                .await().indefinitely();
    }

    @Override
    public void invalidate(String cacheName, String key) {
        cacheManager.getCache(cacheName)
                .ifPresent(c -> c.invalidate(key).await().indefinitely());
    }
}
