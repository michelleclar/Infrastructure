package org.carl.infrastructure.cache.core;

import io.quarkus.cache.CacheManager;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class CacheContextProvider implements Provider<CacheContext> {
    @Inject ReactiveRedisDataSource reactiveRedisDataSource;
    @Inject CacheManager cacheManager;

    // Resolved here (a CDI bean) rather than on RemoteCacheContext: the latter is created via
    // `new` inside CacheContext, so @ConfigProperty would never be injected there.
    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @Produces
    @ApplicationScoped
    @Override
    public CacheContext get() {
        return new CacheContext(cacheManager, reactiveRedisDataSource, applicationName);
    }
}
