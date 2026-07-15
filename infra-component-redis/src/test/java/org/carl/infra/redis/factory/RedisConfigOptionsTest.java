package org.carl.infra.redis.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.json.jackson.DatabindCodec;
import org.junit.jupiter.api.Test;

import java.util.Set;

class RedisConfigOptionsTest {

    @Test
    void registeringModuleDoesNotModifyVertxGlobalMapper() {
        Set<Object> registeredModuleIds = DatabindCodec.mapper().getRegisteredModuleIds();
        RedisConfigOptions options = new RedisConfigOptions();

        options.registerModules(new SimpleModule("redis-only"));

        assertEquals(1, options.getJacksonModules().size());
        assertEquals(registeredModuleIds, DatabindCodec.mapper().getRegisteredModuleIds());
    }
}
