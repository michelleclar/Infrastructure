package org.carl.infrastructure.cache;

import org.carl.infrastructure.cache.core.CacheContext;

public interface ICacheProvider {
    CacheContext getCacheContext();
}
