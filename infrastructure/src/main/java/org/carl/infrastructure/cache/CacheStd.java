package org.carl.infrastructure.cache;

import jakarta.inject.Inject;
import org.carl.infrastructure.cache.core.CacheContext;

public class CacheStd implements ICacheProvider {
    CacheContext context;

    @Override
    public CacheContext getCacheContext() {
        return null;
    }

    @Inject
    public void setContext(CacheContext context) {
        this.context = context;
    }
}
