package org.carl.infrastructure.redis.option;

import com.fasterxml.jackson.databind.Module;

import io.vertx.redis.client.RedisOptions;

public class RedisConfigOptions extends RedisOptions {
    public void registerModules(Module module) {
        io.vertx.core.json.jackson.DatabindCodec.mapper().registerModule(module);
    }
}
