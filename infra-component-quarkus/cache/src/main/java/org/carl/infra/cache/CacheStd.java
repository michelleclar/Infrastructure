package org.carl.infra.cache;

import jakarta.inject.Inject;

import org.carl.infra.cache.core.CacheContext;

public class CacheStd implements ICacheProvider {
    CacheContext context;

    @Override
    public CacheContext getCacheContext() {
        return context;
    }

    @Inject
    public void setContext(CacheContext context) {
        this.context = context;
    }
}
