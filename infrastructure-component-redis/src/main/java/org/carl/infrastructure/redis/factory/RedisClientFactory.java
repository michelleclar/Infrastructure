package org.carl.infrastructure.redis.factory;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;

public class RedisClientFactory {
    public static RedisClient create() {
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx);
        return new RedisClient(client, vertx);
    }

    public static RedisClient create(RedisConfigOptions redisConfigOptions) {
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx, redisConfigOptions.getActualOptions());
        return new RedisClient(client, vertx);
    }
}
