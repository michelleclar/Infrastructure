package org.carl.infrastructure.redis.factory;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.redis.client.Redis;

import java.util.concurrent.atomic.AtomicBoolean;

public class RedisClientFactory {

    private static final AtomicBoolean JACKSON_CONFIGURED = new AtomicBoolean(false);

    private static void ensureJavaTimeModule() {
        if (JACKSON_CONFIGURED.compareAndSet(false, true)) {
            DatabindCodec.mapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    public static RedisClient create() {
        ensureJavaTimeModule();
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx);
        return new RedisClient(client, vertx);
    }

    public static RedisClient create(RedisConfigOptions redisConfigOptions) {
        ensureJavaTimeModule();
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx, redisConfigOptions.getActualOptions());
        return new RedisClient(client, vertx);
    }
}
