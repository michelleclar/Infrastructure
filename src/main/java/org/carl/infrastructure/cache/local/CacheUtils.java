package org.carl.infrastructure.cache.local;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.jboss.logging.Logger;

public class CacheUtils {
    private static final Logger log = Logger.getLogger(CacheUtils.class);

    public static <T> Uni<T> getV(Cache cache, String key, Class<T> type) {
        return Uni.createFrom()
                .item(key)
                .onItem()
                .transform(
                        Unchecked.function(
                                item -> {
                                    CompletableFuture<Object> ifPresent =
                                            cache.as(CaffeineCache.class).getIfPresent(key);
                                    if (ifPresent == null) {
                                        return null;
                                    }
                                    try {
                                        Object o = ifPresent.get();
                                        return type.cast(o);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                }));
    }

    public static void putKV(Cache cache, String key, Object value) {
        cache.as(CaffeineCache.class).put(key, CompletableFuture.completedFuture(value));
    }

    public static void invalidateCache(Cache cache, Object key) {
        cache.invalidate(key)
                .onFailure()
                .call(
                        t -> {
                            log.error(t.getMessage());
                            return Uni.createFrom().nullItem();
                        })
                .subscribe()
                .with(
                        r -> {
                            log.info("invalidate cache success");
                        });
    }
}
