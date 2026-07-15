package org.carl.infra.redis.factory;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import org.carl.infra.redis.codec.JacksonRedisValueCodec;
import org.carl.infra.redis.codec.RedisValueCodec;

import java.util.Objects;

public class RedisClientFactory {

    private static final RedisValueCodec DEFAULT_CODEC = new JacksonRedisValueCodec();

    public static RedisClient create() {
        return create(DEFAULT_CODEC);
    }

    public static RedisClient create(RedisValueCodec codec) {
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx);
        return new RedisClient(client, vertx, Objects.requireNonNull(codec));
    }

    public static RedisClient create(RedisConfigOptions redisConfigOptions) {
        RedisValueCodec codec =
                redisConfigOptions.getJacksonModules().isEmpty()
                        ? DEFAULT_CODEC
                        : new JacksonRedisValueCodec(redisConfigOptions.getJacksonModules());
        return create(redisConfigOptions, codec);
    }

    public static RedisClient create(
            RedisConfigOptions redisConfigOptions, RedisValueCodec codec) {
        Vertx vertx = Vertx.vertx();
        Redis client = Redis.createClient(vertx, redisConfigOptions.getActualOptions());
        return new RedisClient(client, vertx, Objects.requireNonNull(codec));
    }
}
