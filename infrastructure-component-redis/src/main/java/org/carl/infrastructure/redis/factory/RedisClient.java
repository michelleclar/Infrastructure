package org.carl.infrastructure.redis.factory;

import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RedisClient implements AutoCloseable {
    private final Redis redis;
    private final io.vertx.core.Vertx vertx;

    RedisClient(Redis redis, io.vertx.core.Vertx vertx) {
        this.redis = redis;
        this.vertx = vertx;
    }

    /**
     * Get value by key (Async)
     *
     * @param key the key
     * @return Future with the value
     */
    public Future<String> get(String key) {
        return redis.send(Request.cmd(Command.GET).arg(key))
                .map(
                        response -> {
                            if (response == null) {
                                return null;
                            }
                            return response.toString();
                        });
    }

    /**
     * Get value by key (Sync)
     *
     * @param key the key
     * @return the value
     */
    public String getSync(String key) {
        return get(key).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Set value by key (Async)
     *
     * @param key the key
     * @param value the value
     * @return Future
     */
    public Future<Void> set(String key, String value) {
        return redis.send(Request.cmd(Command.SET).arg(key).arg(value)).mapEmpty();
    }

    /**
     * Set value by key (Sync)
     *
     * @param key the key
     * @param value the value
     */
    public void setSync(String key, String value) {
        set(key, value).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Set value by key with expiration (Async)
     *
     * @param key the key
     * @param value the value
     * @param duration expiration duration
     * @return Future
     */
    public Future<Void> set(String key, String value, Duration duration) {
        return redis.send(Request.cmd(Command.SETEX).arg(key).arg(duration.getSeconds()).arg(value))
                .mapEmpty();
    }

    /**
     * Set value by key with expiration (Sync)
     *
     * @param key the key
     * @param value the value
     * @param duration expiration duration
     */
    public void setSync(String key, String value, Duration duration) {
        set(key, value, duration).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Delete value by key (Async)
     *
     * @param key the key
     * @return Future
     */
    public Future<Void> del(String key) {
        return redis.send(Request.cmd(Command.DEL).arg(key)).mapEmpty();
    }

    /**
     * Delete value by key (Sync)
     *
     * @param key the key
     */
    public void delSync(String key) {
        del(key).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Find keys by prefix (Async)
     *
     * @param prefix the prefix
     * @return Future with list of keys
     */
    public Future<List<String>> keys(String prefix) {
        return redis.send(Request.cmd(Command.KEYS).arg(prefix + "*"))
                .map(
                        response -> {
                            List<String> keys = new ArrayList<>();
                            if (response != null) {
                                for (io.vertx.redis.client.Response item : response) {
                                    keys.add(item.toString());
                                }
                            }
                            return keys;
                        });
    }

    /**
     * Find keys by prefix (Sync)
     *
     * @param prefix the prefix
     * @return list of keys
     */
    public List<String> keysSync(String prefix) {
        return keys(prefix).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Increment value by key (Async)
     *
     * @param key the key
     * @return Future with the new value
     */
    public Future<Long> incr(String key) {
        return redis.send(Request.cmd(Command.INCR).arg(key)).map(Response::toLong);
    }

    /**
     * Increment value by key (Sync)
     *
     * @param key the key
     * @return the new value
     */
    public Long incrSync(String key) {
        return incr(key).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Atomic Get or Set (Async) If key exists, returns value. If key does not exist, sets key to
     * value with expiration and returns value.
     *
     * @param key the key
     * @param value the value to set if missing
     * @param duration expiration duration
     * @return Future with the value (existing or new)
     */
    public Future<String> getOrSet(String key, String value, Duration duration) {
        // Lua script:
        // redis.call('GET', KEYS[1]) returns the value if exists
        // If not, SET and return the new value
        String script =
                "local v = redis.call('GET', KEYS[1]) "
                        + "if v then "
                        + "  return v "
                        + "else "
                        + "  redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]) "
                        + "  return ARGV[1] "
                        + "end";

        return redis.send(
                        Request.cmd(Command.EVAL)
                                .arg(script)
                                .arg(1)
                                .arg(key)
                                .arg(value)
                                .arg(duration.getSeconds()))
                .map(Response::toString);
    }

    /**
     * Atomic Get or Set (Sync)
     *
     * @param key the key
     * @param value the value to set if missing
     * @param duration expiration duration
     * @return the value (existing or new)
     */
    public String getOrSetSync(String key, String value, Duration duration) {
        return getOrSet(key, value, duration).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Atomic Increment with Initialization (Async) If key exists, increments by 'by'. If key does
     * not exist, sets key to 'init' value.
     *
     * @param key the key
     * @param by amount to increment by
     * @param init initialization value if key missing
     * @return Future with the new value
     */
    public Future<Long> incr(String key, long by, long init) {
        // Lua script:
        // Check if exists using EXISTS
        String script =
                "if redis.call('EXISTS', KEYS[1]) == 1 then "
                        + "  return redis.call('INCRBY', KEYS[1], ARGV[1]) "
                        + "else "
                        + "  redis.call('SET', KEYS[1], ARGV[2]) "
                        + "  return tonumber(ARGV[2]) "
                        + "end";

        return redis.send(Request.cmd(Command.EVAL).arg(script).arg(1).arg(key).arg(by).arg(init))
                .map(Response::toLong);
    }

    /**
     * Atomic Increment with Initialization (Async) If key exists, increments by 'by'. If key does
     * not exist, sets key to 'init' value.
     *
     * @param key the key
     * @param by amount to increment by
     * @param init initialization value if key missing
     * @return Future with the new value
     */
    public Future<Long> incr(String key, long init) {
        // Lua script:
        // Check if exists using EXISTS
        String script =
                "if redis.call('EXISTS', KEYS[1]) == 1 then "
                        + "  return redis.call('INCRBY', KEYS[1], ARGV[1]) "
                        + "else "
                        + "  redis.call('SET', KEYS[1], ARGV[2]) "
                        + "  return tonumber(ARGV[2]) "
                        + "end";

        return redis.send(Request.cmd(Command.EVAL).arg(script).arg(1).arg(key).arg(1).arg(init))
                .map(Response::toLong);
    }

    /**
     * Atomic Increment with Initialization (Sync)
     *
     * @param key the key
     * @param by amount to increment by
     * @param init initialization value if key missing
     * @return the new value
     */
    public Long incrSync(String key, long by, long init) {
        return incr(key, by, init).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Atomic Increment with Initialization (Sync)
     *
     * @param key the key
     * @param init initialization value if key missing
     * @return the new value
     */
    public Long incrSync(String key, long init) {
        return incr(key, 1, init).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Get value by key (Generic Async)
     *
     * @param key the key
     * @param clazz the class of the object
     * @param <T> the type
     * @return Future with the object
     */
    public <T> Future<T> get(String key, Class<T> clazz) {
        return get(key).map(
                        str -> {
                            if (str == null) return null;
                            return io.vertx.core.json.Json.decodeValue(str, clazz);
                        });
    }

    /**
     * Get value by key (Generic Sync)
     *
     * @param key the key
     * @param clazz the class of the object
     * @param <T> the type
     * @return the object
     */
    public <T> T getSync(String key, Class<T> clazz) {
        return get(key, clazz).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Set value by key (Generic Async)
     *
     * @param key the key
     * @param value the object value
     * @param <T> the type
     * @return Future
     */
    public <T> Future<Void> set(String key, T value) {
        return set(key, io.vertx.core.json.Json.encode(value));
    }

    /**
     * Set value by key (Generic Sync)
     *
     * @param key the key
     * @param value the object value
     * @param <T> the type
     */
    public <T> void setSync(String key, T value) {
        set(key, value).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Set value by key with expiration (Generic Async)
     *
     * @param key the key
     * @param value the object value
     * @param duration expiration duration
     * @param <T> the type
     * @return Future
     */
    public <T> Future<Void> set(String key, T value, Duration duration) {
        return set(key, io.vertx.core.json.Json.encode(value), duration);
    }

    /**
     * Set value by key with expiration (Generic Sync)
     *
     * @param key the key
     * @param value the object value
     * @param duration expiration duration
     * @param <T> the type
     */
    public <T> void setSync(String key, T value, Duration duration) {
        set(key, value, duration).toCompletionStage().toCompletableFuture().join();
    }

    /**
     * Atomic Get or Set (Generic Async)
     *
     * @param key the key
     * @param value the object value to set if missing
     * @param duration expiration duration
     * @param clazz the class of the object
     * @param <T> the type
     * @return Future with the object (existing or new)
     */
    public <T> Future<T> getOrSet(String key, T value, Duration duration, Class<T> clazz) {
        return getOrSet(key, io.vertx.core.json.Json.encode(value), duration)
                .map(
                        str -> {
                            if (str == null) return null;
                            return io.vertx.core.json.Json.decodeValue(str, clazz);
                        });
    }

    /**
     * Atomic Get or Set (Generic Sync)
     *
     * @param key the key
     * @param value the object value to set if missing
     * @param duration expiration duration
     * @param clazz the class of the object
     * @param <T> the type
     * @return the object (existing or new)
     */
    public <T> T getOrSetSync(String key, T value, Duration duration, Class<T> clazz) {
        return getOrSet(key, value, duration, clazz)
                .toCompletionStage()
                .toCompletableFuture()
                .join();
    }

    /**
     * Get a distributed lock instance for a key.
     *
     * @param key the lock key
     * @return RedisLock instance
     */
    public RedisLock getLock(String key) {
        return new RedisLock(redis, vertx, key);
    }

    @Override
    public void close() throws Exception {
        redis.close();
    }

    public static class RedisLock {
        private final Redis redis;
        private final io.vertx.core.Vertx vertx;
        private final String lockKey;
        private final String lockValue;
        private final java.util.concurrent.atomic.AtomicLong timerId =
                new java.util.concurrent.atomic.AtomicLong(-1);
        private volatile boolean isLocked = false;

        // Scripts
        private static final String UNLOCK_SCRIPT =
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "return redis.call('del', KEYS[1]) "
                        + "else "
                        + "return 0 "
                        + "end";

        private static final String RENEW_SCRIPT =
                "if redis.call('get', KEYS[1]) == ARGV[1] then "
                        + "return redis.call('pexpire', KEYS[1], ARGV[2]) "
                        + "else "
                        + "return 0 "
                        + "end";

        RedisLock(Redis redis, io.vertx.core.Vertx vertx, String lockKey) {
            this.redis = redis;
            this.vertx = vertx;
            this.lockKey = lockKey;
            this.lockValue = java.util.UUID.randomUUID().toString();
        }

        /**
         * Try to acquire the lock with a specific lease time. No auto-renewal.
         *
         * @param waitTime Max time to wait for lock (ms)
         * @param leaseTime Time until lock expires (ms)
         * @return Future<Boolean> true if acquired
         */
        public Future<Boolean> tryLock(long waitTime, long leaseTime) {
            return tryLockInternal(waitTime, leaseTime, false);
        }

        /**
         * Try to acquire the lock with auto-renewal (watchdog). The lock will be renewed
         * periodically as long as this instance is alive and not unlocked.
         *
         * @param waitTime Max time to wait for lock (ms)
         * @return Future<Boolean> true if acquired
         */
        public Future<Boolean> tryLock(long waitTime) {
            // Default lease time for watchdog is 30 seconds, renew every 10 seconds
            return tryLockInternal(waitTime, 30000, true);
        }

        private Future<Boolean> tryLockInternal(
                long waitTime, long leaseTime, boolean withWatchdog) {
            long start = System.currentTimeMillis();

            return tryAcquire(leaseTime, withWatchdog)
                    .compose(
                            acquired -> {
                                if (acquired) {
                                    return Future.succeededFuture(true);
                                }

                                // If not acquired, check if we still have wait time
                                long elapsed = System.currentTimeMillis() - start;
                                if (elapsed >= waitTime) {
                                    return Future.succeededFuture(false);
                                }

                                // Retry after delay
                                return Future.future(
                                        promise -> {
                                            vertx.setTimer(
                                                    100,
                                                    id -> { // Retry every 100ms
                                                        tryLockInternal(
                                                                        waitTime
                                                                                - (System
                                                                                                .currentTimeMillis()
                                                                                        - start),
                                                                        leaseTime,
                                                                        withWatchdog)
                                                                .onComplete(promise);
                                                    });
                                        });
                            });
        }

        private Future<Boolean> tryAcquire(long leaseTime, boolean withWatchdog) {
            // SET key value NX PX leaseTime
            Request req =
                    Request.cmd(Command.SET)
                            .arg(lockKey)
                            .arg(lockValue)
                            .arg("NX")
                            .arg("PX")
                            .arg(leaseTime);

            return redis.send(req)
                    .map(
                            resp -> {
                                if (resp != null && "OK".equals(resp.toString())) {
                                    isLocked = true;
                                    if (withWatchdog) {
                                        startWatchdog(leaseTime);
                                    }
                                    return true;
                                }
                                return false;
                            });
        }

        private void startWatchdog(long leaseTime) {
            // Renew at 1/3 of lease time
            long delay = leaseTime / 3;

            long id =
                    vertx.setPeriodic(
                            delay,
                            t -> {
                                if (!isLocked) {
                                    vertx.cancelTimer(t);
                                    return;
                                }

                                renewLock(leaseTime)
                                        .onSuccess(
                                                renewed -> {
                                                    if (!renewed) {
                                                        // Lost lock
                                                        isLocked = false;
                                                        vertx.cancelTimer(t);
                                                    }
                                                });
                            });
            timerId.set(id);
        }

        private Future<Boolean> renewLock(long leaseTime) {
            Request req =
                    Request.cmd(Command.EVAL)
                            .arg(RENEW_SCRIPT)
                            .arg(1)
                            .arg(lockKey)
                            .arg(lockValue)
                            .arg(leaseTime);

            return redis.send(req).map(resp -> resp != null && resp.toInteger() == 1);
        }

        /** Unlock and stop watchdog if active. */
        public Future<Void> unlock() {
            long tId = timerId.get();
            if (tId != -1) {
                vertx.cancelTimer(tId);
                timerId.set(-1);
            }
            isLocked = false;

            Request req =
                    Request.cmd(Command.EVAL).arg(UNLOCK_SCRIPT).arg(1).arg(lockKey).arg(lockValue);

            return redis.send(req).mapEmpty();
        }
    }
}
