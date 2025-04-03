package org.carl.infrastructure.cache.core;

import io.quarkus.cache.CacheManager;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class CacheContextProvider implements Provider<CacheContext> {
    @Inject ReactiveRedisDataSource reactiveRedisDataSource;
    @Inject CacheManager cacheManager;

    @Produces
    @ApplicationScoped
    @Override
    public CacheContext get() {
        return new CacheContext(cacheManager, reactiveRedisDataSource);
    }
}
