package org.carl.infrastructure.cache.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.cache.CacheManager;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;

import java.util.ArrayList;
import java.util.List;

public class CacheContext {
    ReactiveRedisDataSource reactiveRedisDataSource;
    public RemoteCacheContext remoteCacheContext;
    public LocalCacheContext localCacheContext;

    public CacheContext(
            CacheManager cacheManager,
            ReactiveRedisDataSource reactiveRedisDataSource,
            String prefix) {
        this.reactiveRedisDataSource = reactiveRedisDataSource;
        this.remoteCacheContext =
                new RemoteCacheContext(
                        reactiveRedisDataSource.key(),
                        reactiveRedisDataSource.value(Object.class),
                        prefix);
        this.localCacheContext = new LocalCacheContext();
    }

    public static class LocalCacheContext implements BaseCache {
        Cache<String, Object> localCache;

        LocalCacheContext() {
            localCache =
                    Caffeine.newBuilder()
                            .initialCapacity(100)
                            .maximumSize(100000)
                            //                            .expireAfterWrite(17, TimeUnit.SECONDS)
                            // .expireAfterAccess(17, TimeUnit.SECONDS)
                            .build();
        }

        @Override
        public Uni<Object> get(String key) {
            return Uni.createFrom().item(localCache.get(key, k -> null));
        }

        @Override
        public Uni<Void> del(String key) {
            localCache.invalidate(key);
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<Void> set(String key, Object v) {
            localCache.put(key, v);
            return Uni.createFrom().voidItem();
        }

        @Override
        public Uni<List<String>> keys() {
            List<String> keys = new ArrayList<>();
            localCache.asMap().forEach((k, v) -> keys.add(k));
            return Uni.createFrom().item(keys);
        }
    }

    public static class RemoteCacheContext implements BaseCache {
        private final ReactiveKeyCommands<String> reactiveKeyCommands;

        private final ReactiveValueCommands<String, Object> reactiveValueCommands;

        private final String prefix;

        RemoteCacheContext(
                ReactiveKeyCommands<String> reactiveKeyCommands,
                ReactiveValueCommands<String, Object> reactiveValueCommands,
                String prefix) {
            this.reactiveKeyCommands = reactiveKeyCommands;
            this.reactiveValueCommands = reactiveValueCommands;
            this.prefix = prefix;
        }

        @Override
        public Uni<Object> get(String key) {
            String _key = prefix + ":" + key;
            return reactiveValueCommands.get(_key);
        }

        @Override
        public Uni<Void> del(String key) {
            String _key = prefix + ":" + key;
            return reactiveKeyCommands.del(_key).replaceWithVoid();
        }

        @Override
        public Uni<Void> set(String key, Object value) {
            String _key = prefix + ":" + key;
            return reactiveValueCommands.set(_key, value).replaceWithVoid();
        }

        public Uni<Boolean> setExpireAfterWrite(String key, long seconds, Object value) {
            String _key = prefix + ":" + key;
            return reactiveValueCommands
                    .setnx(_key, value)
                    .onItem()
                    .call(
                            o -> {
                                if (!o) return Uni.createFrom().item(false);
                                return this.reactiveKeyCommands.expire(_key, seconds);
                            });
        }

        @Override
        public Uni<List<String>> keys() {
            return reactiveKeyCommands.keys(prefix + "*");
        }
    }
}
