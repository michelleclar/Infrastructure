package org.carl.infra.cache;

import org.carl.infra.cache.core.CacheContext;

public interface ICacheProvider {
    CacheContext getCacheContext();
}
