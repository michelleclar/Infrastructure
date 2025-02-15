package org.carl.infrastructure.cache;

import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.cache.enable", stringValue = "true")
public class CacheService extends CacheStd implements ICacheOperations {}
