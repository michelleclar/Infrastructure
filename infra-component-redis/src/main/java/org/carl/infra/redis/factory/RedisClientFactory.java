package org.carl.infra.redis.factory;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import org.carl.infra.redis.codec.JacksonRedisValueCodec;
import org.carl.infra.redis.codec.RedisValueCodec;

import java.util.Objects;

public class RedisClientFactory {

    private static final RedisValueCodec DEFAULT_CODEC = new JacksonRedisValueCodec();

    public static RedisClient create() {
        return create(new RedisConfigOptions(), DEFAULT_CODEC);
    }

    public static RedisClient create(RedisValueCodec codec) {
        return create(new RedisConfigOptions(), codec);
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
        try {
            return create(vertx, redisConfigOptions, codec, true);
        } catch (RuntimeException exception) {
            vertx.close();
            throw exception;
        }
    }

    public static RedisClient create(Vertx vertx) {
        return create(vertx, new RedisConfigOptions(), DEFAULT_CODEC, false);
    }

    public static RedisClient create(Vertx vertx, RedisValueCodec codec) {
        return create(vertx, new RedisConfigOptions(), codec, false);
    }

    public static RedisClient create(Vertx vertx, RedisConfigOptions redisConfigOptions) {
        Objects.requireNonNull(redisConfigOptions, "redisConfigOptions must not be null");
        RedisValueCodec codec =
                redisConfigOptions.getJacksonModules().isEmpty()
                        ? DEFAULT_CODEC
                        : new JacksonRedisValueCodec(redisConfigOptions.getJacksonModules());
        return create(vertx, redisConfigOptions, codec, false);
    }

    public static RedisClient create(
            Vertx vertx, RedisConfigOptions redisConfigOptions, RedisValueCodec codec) {
        return create(vertx, redisConfigOptions, codec, false);
    }

    private static RedisClient create(
            Vertx vertx,
            RedisConfigOptions redisConfigOptions,
            RedisValueCodec codec,
            boolean ownsVertx) {
        Objects.requireNonNull(vertx, "vertx must not be null");
        Objects.requireNonNull(redisConfigOptions, "redisConfigOptions must not be null");
        Objects.requireNonNull(codec, "codec must not be null");
        RedisOptions actualOptions = redisConfigOptions.getActualOptions();
        Redis client = Redis.createClient(vertx, actualOptions);
        return new RedisClient(
                client,
                vertx,
                codec,
                redisConfigOptions.getCommandTimeout(),
                ownsVertx,
                actualOptions.getType());
    }
}
