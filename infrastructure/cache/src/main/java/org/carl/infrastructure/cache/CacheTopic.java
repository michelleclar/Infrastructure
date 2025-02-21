package org.carl.infrastructure.cache;

public enum CacheTopic {
    SYSTEM("system", CacheType.MAP),
    ;

    CacheTopic(String field, CacheType type) {
        this.cacheName = field + ":";
        this.cacheType = type;
    }

    public final CacheType cacheType;

    public final String cacheName;
}
