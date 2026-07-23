package org.carl.infra.redis.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import org.carl.infra.redis.codec.RedisValueCodec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class RedisClient implements AutoCloseable {
    private static final String GET_OR_SET_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) "
                    + "if v then "
                    + "  return v "
                    + "else "
                    + "  redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]) "
                    + "  return ARGV[1] "
                    + "end";

    private static final String INCREMENT_WITH_INITIAL_VALUE_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 1 then "
                    + "  return redis.call('INCRBY', KEYS[1], ARGV[1]) "
                    + "else "
                    + "  redis.call('SET', KEYS[1], ARGV[2]) "
                    + "  return tonumber(ARGV[2]) "
                    + "end";

    private final Redis redis;
    private final Vertx vertx;
    private final RedisValueCodec codec;
    private final Duration commandTimeout;
    private final boolean ownsVertx;
    private final RedisClientType clientType;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<CompletableFuture<Void>> closeFuture = new AtomicReference<>();
    private final Set<RedisLock> activeLocks = ConcurrentHashMap.newKeySet();

    RedisClient(
            Redis redis,
            Vertx vertx,
            RedisValueCodec codec,
            Duration commandTimeout,
            boolean ownsVertx,
            RedisClientType clientType) {
        this.redis = Objects.requireNonNull(redis, "redis must not be null");
        this.vertx = Objects.requireNonNull(vertx, "vertx must not be null");
        this.codec = Objects.requireNonNull(codec, "codec must not be null");
        this.commandTimeout = requirePositiveDuration(commandTimeout, "commandTimeout");
        this.ownsVertx = ownsVertx;
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
    }

    /**
     * Get value by key (Async).
     *
     * @param key the key
     * @return future with the value
     */
    public CompletableFuture<String> get(String key) {
        return execute(Request.cmd(Command.GET).arg(requireKey(key)))
                .map(response -> response == null ? null : response.toString())
                .toCompletionStage()
                .toCompletableFuture();
    }

    /**
     * Get and convert a value by key (Async).
     *
     * @param key the key
     * @param function value converter
     * @return future with the converted value
     */
    public <T> CompletableFuture<T> get(String key, Function<String, T> function) {
        Objects.requireNonNull(function, "function must not be null");
        return get(key).thenApply(value -> value == null ? null : function.apply(value));
    }

    public String getSync(String key) {
        return get(key).join();
    }

    public CompletableFuture<Response> set(String key, String value) {
        Objects.requireNonNull(value, "value must not be null");
        return execute(Request.cmd(Command.SET).arg(requireKey(key)).arg(value))
                .toCompletionStage()
                .toCompletableFuture();
    }

    public void setSync(String key, String value) {
        set(key, value).join();
    }

    public CompletableFuture<Response> set(String key, String value, Duration duration) {
        Objects.requireNonNull(value, "value must not be null");
        long expirationMillis = positiveMillis(duration, "duration");
        return execute(
                        Request.cmd(Command.SET)
                                .arg(requireKey(key))
                                .arg(value)
                                .arg("PX")
                                .arg(expirationMillis))
                .toCompletionStage()
                .toCompletableFuture();
    }

    public void setSync(String key, String value, Duration duration) {
        set(key, value, duration).join();
    }

    public CompletableFuture<Response> del(String key) {
        return execute(Request.cmd(Command.DEL).arg(requireKey(key)))
                .toCompletionStage()
                .toCompletableFuture();
    }

    public void delSync(String key) {
        del(key).join();
    }

    public CompletableFuture<Response> del(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        Request request = Request.cmd(Command.DEL);
        for (String key : keys) {
            request.arg(requireKey(key));
        }
        return execute(request).toCompletionStage().toCompletableFuture();
    }

    public void delSync(List<String> keys) {
        del(keys).join();
    }

    public CompletableFuture<Long> pttl(String key) {
        return execute(Request.cmd(Command.PTTL).arg(requireKey(key)))
                .map(Response::toLong)
                .toCompletionStage()
                .toCompletableFuture();
    }

    public Long pttlSync(String key) {
        return pttl(key).join();
    }

    /**
     * Scan one page of keys whose names start with {@code prefix}.
     *
     * <p>This operation is intentionally rejected for Redis Cluster because the connection-less
     * Vert.x {@code SCAN} command only targets one cluster node.
     */
    public CompletableFuture<ScanPage> scan(String prefix, String cursor) {
        return scan(prefix, cursor, null);
    }

    /**
     * Scan one page of keys whose names start with {@code prefix}.
     *
     * @param prefix literal key prefix
     * @param cursor Redis scan cursor, use {@code "0"} for the first page
     * @param count positive Redis COUNT hint
     */
    public CompletableFuture<ScanPage> scan(String prefix, String cursor, int count) {
        if (count <= 0) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("count must be greater than 0"));
        }
        return scan(prefix, cursor, Integer.valueOf(count));
    }

    private CompletableFuture<ScanPage> scan(String prefix, String cursor, Integer count) {
        if (clientType == RedisClientType.CLUSTER) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException(
                            "SCAN across all Redis Cluster nodes is not supported"));
        }
        Objects.requireNonNull(prefix, "prefix must not be null");
        if (cursor == null || cursor.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("cursor must not be blank"));
        }

        Request request =
                Request.cmd(Command.SCAN)
                        .arg(cursor)
                        .arg("MATCH")
                        .arg(escapeRedisGlobLiteral(prefix) + "*");
        if (count != null) {
            request.arg("COUNT").arg(count);
        }

        return execute(request)
                .map(RedisClient::toScanPage)
                .toCompletionStage()
                .toCompletableFuture();
    }

    /**
     * Collect all keys whose names start with {@code prefix} using Redis SCAN.
     *
     * <p>Prefer the paged {@link #scan(String, String)} API when the result set can be large.
     */
    public CompletableFuture<List<String>> keys(String prefix) {
        List<String> result = new ArrayList<>();
        return scanAll(prefix, "0", result);
    }

    public List<String> keysSync(String prefix) {
        return keys(prefix).join();
    }

    private CompletableFuture<List<String>> scanAll(
            String prefix, String cursor, List<String> result) {
        return scan(prefix, cursor)
                .thenCompose(
                        page -> {
                            result.addAll(page.keys());
                            if ("0".equals(page.cursor())) {
                                return CompletableFuture.completedFuture(List.copyOf(result));
                            }
                            return scanAll(prefix, page.cursor(), result);
                        });
    }

    public CompletableFuture<Long> incr(String key) {
        return execute(Request.cmd(Command.INCR).arg(requireKey(key)))
                .map(Response::toLong)
                .toCompletionStage()
                .toCompletableFuture();
    }

    public Long incrSync(String key) {
        return incr(key).join();
    }

    public CompletableFuture<String> getOrSet(String key, String value, Duration duration) {
        Objects.requireNonNull(value, "value must not be null");
        long expirationMillis = positiveMillis(duration, "duration");
        return execute(
                        Request.cmd(Command.EVAL)
                                .arg(GET_OR_SET_SCRIPT)
                                .arg(1)
                                .arg(requireKey(key))
                                .arg(value)
                                .arg(expirationMillis))
                .map(response -> response == null ? null : response.toString())
                .toCompletionStage()
                .toCompletableFuture();
    }

    public String getOrSetSync(String key, String value, Duration duration) {
        return getOrSet(key, value, duration).join();
    }

    public CompletableFuture<Long> incr(String key, long by, long init) {
        return execute(
                        Request.cmd(Command.EVAL)
                                .arg(INCREMENT_WITH_INITIAL_VALUE_SCRIPT)
                                .arg(1)
                                .arg(requireKey(key))
                                .arg(by)
                                .arg(init))
                .map(Response::toLong)
                .toCompletionStage()
                .toCompletableFuture();
    }

    public CompletableFuture<Long> incr(String key, long init) {
        return incr(key, 1, init);
    }

    public Long incrSync(String key, long by, long init) {
        return incr(key, by, init).join();
    }

    public Long incrSync(String key, long init) {
        return incr(key, 1, init).join();
    }

    public <T> CompletableFuture<T> get(String key, Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        return get(key).thenApply(value -> value == null ? null : codec.decode(value, clazz));
    }

    public <T> T getSync(String key, Class<T> clazz) {
        return get(key, clazz).join();
    }

    public <T> CompletableFuture<T> get(String key, TypeReference<T> typeRef) {
        Objects.requireNonNull(typeRef, "typeRef must not be null");
        return get(key).thenApply(value -> value == null ? null : codec.decode(value, typeRef));
    }

    public <T> T getSync(String key, TypeReference<T> typeRef) {
        return get(key, typeRef).join();
    }

    public <T> CompletableFuture<Response> set(String key, T value) {
        return set(key, codec.encode(value));
    }

    public <T> void setSync(String key, T value) {
        set(key, value).join();
    }

    public <T> CompletableFuture<Response> set(String key, T value, Duration duration) {
        return set(key, codec.encode(value), duration);
    }

    public <T> void setSync(String key, T value, Duration duration) {
        set(key, value, duration).join();
    }

    public <T> CompletableFuture<T> getOrSet(
            String key, T value, Duration duration, Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        return getOrSet(key, codec.encode(value), duration)
                .thenApply(encoded -> encoded == null ? null : codec.decode(encoded, clazz));
    }

    public <T> T getOrSetSync(String key, T value, Duration duration, Class<T> clazz) {
        return getOrSet(key, value, duration, clazz).join();
    }

    public RedisLock getLock(String key) {
        return new RedisLock(this, vertx, requireKey(key));
    }

    /**
     * Close the Redis client and, when this client created it, the owned Vert.x instance.
     *
     * @return future completed when owned resources are closed
     */
    public CompletableFuture<Void> closeAsync() {
        CompletableFuture<Void> existing = closeFuture.get();
        if (existing != null) {
            return existing;
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        if (!closeFuture.compareAndSet(null, result)) {
            return closeFuture.get();
        }

        closed.set(true);
        for (RedisLock lock : List.copyOf(activeLocks)) {
            lock.handleClientClose();
        }

        try {
            redis.close();
            if (ownsVertx) {
                vertx.close().onComplete(
                                closeResult -> {
                                    if (closeResult.succeeded()) {
                                        result.complete(null);
                                    } else {
                                        result.completeExceptionally(closeResult.cause());
                                    }
                                });
            } else {
                result.complete(null);
            }
        } catch (Throwable throwable) {
            result.completeExceptionally(throwable);
        }
        return result;
    }

    @Override
    public void close() {
        CompletableFuture<Void> closing = closeAsync();
        Context currentContext = Vertx.currentContext();
        if (currentContext != null && currentContext.owner() == vertx) {
            return;
        }
        closing.join();
    }

    private Future<Response> execute(Request request) {
        if (closed.get()) {
            return Future.failedFuture(new IllegalStateException("RedisClient is closed"));
        }
        return redis.send(request).timeout(commandTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    Vertx vertxInstance() {
        return vertx;
    }

    boolean ownsVertxInstance() {
        return ownsVertx;
    }

    private static ScanPage toScanPage(Response response) {
        if (response == null || response.size() != 2 || response.get(1) == null) {
            throw new IllegalStateException("Unexpected Redis SCAN response");
        }
        List<String> keys = new ArrayList<>();
        for (Response key : response.get(1)) {
            keys.add(key.toString());
        }
        return new ScanPage(response.get(0).toString(), List.copyOf(keys));
    }

    private static String escapeRedisGlobLiteral(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '*' || ch == '?' || ch == '[' || ch == ']' || ch == '\\') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    private static String requireKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be null or empty");
        }
        return key;
    }

    private static Duration requirePositiveDuration(Duration duration, String name) {
        positiveMillis(duration, name);
        return duration;
    }

    private static long positiveMillis(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
        try {
            long millis = duration.toMillis();
            if (millis <= 0) {
                throw new IllegalArgumentException(name + " must be at least 1 millisecond");
            }
            return millis;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " is too large", e);
        }
    }

    public record ScanPage(String cursor, List<String> keys) {
        public ScanPage {
            Objects.requireNonNull(cursor, "cursor must not be null");
            keys = List.copyOf(Objects.requireNonNull(keys, "keys must not be null"));
        }
    }

    public static final class RedisLock {
        private static final long WATCHDOG_LEASE_MILLIS = 30_000;
        private static final long RETRY_INTERVAL_MILLIS = 100;

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

        private final RedisClient client;
        private final Vertx vertx;
        private final String lockKey;
        private final AtomicReference<LockState> state = new AtomicReference<>(LockState.IDLE);
        private final AtomicLong retryTimerId = new AtomicLong(-1);
        private final AtomicLong leaseTimerId = new AtomicLong(-1);
        private final AtomicLong watchdogTimerId = new AtomicLong(-1);
        private final AtomicBoolean renewalInFlight = new AtomicBoolean();

        private volatile String lockValue;
        private volatile CompletableFuture<Boolean> acquisitionFuture;
        private volatile CompletableFuture<LockTermination> terminationFuture =
                CompletableFuture.completedFuture(LockTermination.RELEASED);

        RedisLock(RedisClient client, Vertx vertx, String lockKey) {
            this.client = client;
            this.vertx = vertx;
            this.lockKey = lockKey;
        }

        public CompletableFuture<Boolean> tryLock(long waitTime, long leaseTime) {
            if (waitTime < 0) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("waitTime must not be negative"));
            }
            if (leaseTime <= 0) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("leaseTime must be greater than 0"));
            }
            return beginAcquire(waitTime, leaseTime, false);
        }

        public CompletableFuture<Boolean> tryLock(Duration waitTime, Duration leaseTime) {
            Objects.requireNonNull(waitTime, "waitTime must not be null");
            if (waitTime.isNegative()) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("waitTime must not be negative"));
            }
            return tryLock(toMillisAllowZero(waitTime, "waitTime"), positiveMillis(leaseTime, "leaseTime"));
        }

        public CompletableFuture<Boolean> tryLock(long waitTime) {
            if (waitTime < 0) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("waitTime must not be negative"));
            }
            return beginAcquire(waitTime, WATCHDOG_LEASE_MILLIS, true);
        }

        public CompletableFuture<Boolean> tryLock(Duration waitTime) {
            Objects.requireNonNull(waitTime, "waitTime must not be null");
            if (waitTime.isNegative()) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("waitTime must not be negative"));
            }
            return tryLock(toMillisAllowZero(waitTime, "waitTime"));
        }

        /**
         * Completes with {@link LockTermination#LOST} when the lease expires or renewal fails, and
         * with {@link LockTermination#RELEASED} after a successful unlock.
         */
        public CompletableFuture<LockTermination> terminationFuture() {
            return terminationFuture;
        }

        public boolean isHeldByThisInstance() {
            return state.get() == LockState.LOCKED;
        }

        public CompletableFuture<Response> unlock() {
            LockState current;
            do {
                current = state.get();
                if (current != LockState.LOCKED && current != LockState.LOST) {
                    return CompletableFuture.failedFuture(
                            new IllegalStateException(
                                    "Lock cannot be unlocked while in state " + current));
                }
            } while (!state.compareAndSet(current, LockState.RELEASING));

            cancelTimers();
            String value = lockValue;
            CompletableFuture<Response> result =
                    client.execute(
                                    Request.cmd(Command.EVAL)
                                            .arg(UNLOCK_SCRIPT)
                                            .arg(1)
                                            .arg(lockKey)
                                            .arg(value))
                            .toCompletionStage()
                            .toCompletableFuture();

            result.whenComplete(
                    (response, failure) -> {
                        if (failure == null) {
                            state.set(LockState.IDLE);
                            lockValue = null;
                            terminationFuture.complete(LockTermination.RELEASED);
                        } else {
                            state.set(LockState.LOST);
                            terminationFuture.complete(LockTermination.LOST);
                        }
                        client.activeLocks.remove(this);
                    });
            return result;
        }

        private CompletableFuture<Boolean> beginAcquire(
                long waitTimeMillis, long leaseTimeMillis, boolean withWatchdog) {
            if (!state.compareAndSet(LockState.IDLE, LockState.ACQUIRING)) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "RedisLock is already in use; current state is " + state.get()));
            }
            if (client.closed.get()) {
                state.set(LockState.CLOSED);
                return CompletableFuture.failedFuture(
                        new IllegalStateException("RedisClient is closed"));
            }

            lockValue = UUID.randomUUID().toString();
            terminationFuture = new CompletableFuture<>();
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            acquisitionFuture = result;
            client.activeLocks.add(this);

            long waitNanos = TimeUnit.MILLISECONDS.toNanos(waitTimeMillis);
            long startedAt = System.nanoTime();
            long deadline =
                    Long.MAX_VALUE - startedAt < waitNanos
                            ? Long.MAX_VALUE
                            : startedAt + waitNanos;

            String acquisitionToken = lockValue;
            attemptAcquire(
                    result,
                    deadline,
                    leaseTimeMillis,
                    withWatchdog,
                    acquisitionToken);
            result.whenComplete(
                    (acquired, failure) -> {
                        if (result.isCancelled()
                                && state.compareAndSet(LockState.ACQUIRING, LockState.IDLE)) {
                            cancelRetryTimer();
                            lockValue = null;
                            terminationFuture.complete(LockTermination.RELEASED);
                            client.activeLocks.remove(this);
                        }
                    });
            return result;
        }

        private void attemptAcquire(
                CompletableFuture<Boolean> result,
                long deadline,
                long leaseTimeMillis,
                boolean withWatchdog,
                String acquisitionToken) {
            if (result.isDone() || state.get() != LockState.ACQUIRING) {
                return;
            }

            tryAcquire(acquisitionToken, leaseTimeMillis)
                    .whenComplete(
                            (acquired, failure) -> {
                                if (Boolean.TRUE.equals(acquired)
                                        && (result.isDone()
                                                || state.get() != LockState.ACQUIRING)) {
                                    releaseTokenBestEffort(acquisitionToken);
                                    return;
                                }
                                if (result.isDone()
                                        || state.get() != LockState.ACQUIRING) {
                                    return;
                                }
                                if (failure != null) {
                                    finishAcquireFailure(result, failure);
                                    return;
                                }
                                if (Boolean.TRUE.equals(acquired)) {
                                    if (state.compareAndSet(
                                            LockState.ACQUIRING, LockState.LOCKED)) {
                                        try {
                                            if (withWatchdog) {
                                                startWatchdog(leaseTimeMillis);
                                            } else {
                                                startLeaseTimer(leaseTimeMillis);
                                            }
                                            result.complete(true);
                                        } catch (Throwable schedulingFailure) {
                                            markLost();
                                            result.completeExceptionally(schedulingFailure);
                                        }
                                    }
                                    return;
                                }

                                long remainingNanos = deadline - System.nanoTime();
                                if (remainingNanos <= 0) {
                                    finishNotAcquired(result);
                                    return;
                                }

                                long remainingMillis =
                                        Math.max(
                                                1,
                                                TimeUnit.NANOSECONDS.toMillis(remainingNanos));
                                long delay = Math.min(RETRY_INTERVAL_MILLIS, remainingMillis);
                                try {
                                    long timerId =
                                            vertx.setTimer(
                                                    delay,
                                                    ignored -> {
                                                        retryTimerId.set(-1);
                                                        attemptAcquire(
                                                                result,
                                                                deadline,
                                                                leaseTimeMillis,
                                                                withWatchdog,
                                                                acquisitionToken);
                                                    });
                                    retryTimerId.set(timerId);
                                    if (result.isDone()) {
                                        cancelRetryTimer();
                                    }
                                } catch (Throwable schedulingFailure) {
                                    finishAcquireFailure(result, schedulingFailure);
                                }
                            });
        }

        private CompletableFuture<Boolean> tryAcquire(
                String acquisitionToken, long leaseTimeMillis) {
            return client.execute(
                            Request.cmd(Command.SET)
                                    .arg(lockKey)
                                    .arg(acquisitionToken)
                                    .arg("NX")
                                    .arg("PX")
                                    .arg(leaseTimeMillis))
                    .map(response -> response != null && "OK".equals(response.toString()))
                    .toCompletionStage()
                    .toCompletableFuture();
        }

        private void releaseTokenBestEffort(String acquisitionToken) {
            if (client.closed.get()) {
                return;
            }
            client.execute(
                            Request.cmd(Command.EVAL)
                                    .arg(UNLOCK_SCRIPT)
                                    .arg(1)
                                    .arg(lockKey)
                                    .arg(acquisitionToken))
                    .onFailure(ignored -> {});
        }

        private void finishNotAcquired(CompletableFuture<Boolean> result) {
            if (state.compareAndSet(LockState.ACQUIRING, LockState.IDLE)) {
                lockValue = null;
                terminationFuture.complete(LockTermination.RELEASED);
                client.activeLocks.remove(this);
            }
            result.complete(false);
        }

        private void finishAcquireFailure(
                CompletableFuture<Boolean> result, Throwable failure) {
            if (state.compareAndSet(LockState.ACQUIRING, LockState.IDLE)) {
                lockValue = null;
                terminationFuture.complete(LockTermination.RELEASED);
                client.activeLocks.remove(this);
            }
            result.completeExceptionally(failure);
        }

        private void startLeaseTimer(long leaseTimeMillis) {
            leaseTimerId.set(
                    vertx.setTimer(
                            leaseTimeMillis,
                            ignored -> {
                                leaseTimerId.set(-1);
                                markLost();
                            }));
        }

        private void startWatchdog(long leaseTimeMillis) {
            long renewalIntervalMillis = Math.max(1, leaseTimeMillis / 3);
            watchdogTimerId.set(
                    vertx.setPeriodic(
                            renewalIntervalMillis,
                            ignored -> {
                                if (state.get() != LockState.LOCKED) {
                                    cancelWatchdogTimer();
                                    return;
                                }
                                if (!renewalInFlight.compareAndSet(false, true)) {
                                    return;
                                }
                                renewLock(leaseTimeMillis)
                                        .whenComplete(
                                                (renewed, failure) -> {
                                                    renewalInFlight.set(false);
                                                    if (failure != null
                                                            || !Boolean.TRUE.equals(renewed)) {
                                                        markLost();
                                                    }
                                                });
                            }));
        }

        private CompletableFuture<Boolean> renewLock(long leaseTimeMillis) {
            return client.execute(
                            Request.cmd(Command.EVAL)
                                    .arg(RENEW_SCRIPT)
                                    .arg(1)
                                    .arg(lockKey)
                                    .arg(lockValue)
                                    .arg(leaseTimeMillis))
                    .map(response -> response != null && response.toInteger() == 1)
                    .toCompletionStage()
                    .toCompletableFuture();
        }

        private void markLost() {
            if (state.compareAndSet(LockState.LOCKED, LockState.LOST)) {
                cancelTimers();
                terminationFuture.complete(LockTermination.LOST);
                client.activeLocks.remove(this);
            }
        }

        private void handleClientClose() {
            LockState previous = state.getAndSet(LockState.CLOSED);
            cancelTimers();
            CompletableFuture<Boolean> acquiring = acquisitionFuture;
            if (acquiring != null && !acquiring.isDone()) {
                acquiring.completeExceptionally(
                        new IllegalStateException("RedisClient closed while acquiring lock"));
            }
            if (previous == LockState.LOCKED
                    || previous == LockState.LOST
                    || previous == LockState.RELEASING) {
                terminationFuture.complete(LockTermination.LOST);
            } else {
                terminationFuture.complete(LockTermination.RELEASED);
            }
            lockValue = null;
            client.activeLocks.remove(this);
        }

        private void cancelTimers() {
            cancelRetryTimer();
            cancelTimer(leaseTimerId);
            cancelWatchdogTimer();
        }

        private void cancelRetryTimer() {
            cancelTimer(retryTimerId);
        }

        private void cancelWatchdogTimer() {
            cancelTimer(watchdogTimerId);
        }

        private void cancelTimer(AtomicLong timerId) {
            long id = timerId.getAndSet(-1);
            if (id != -1) {
                vertx.cancelTimer(id);
            }
        }

        private static long toMillisAllowZero(Duration duration, String name) {
            try {
                long millis = duration.toMillis();
                if (!duration.isZero() && millis == 0) {
                    throw new IllegalArgumentException(
                            name + " must be zero or at least 1 millisecond");
                }
                return millis;
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(name + " is too large", e);
            }
        }

        public enum LockTermination {
            RELEASED,
            LOST
        }

        private enum LockState {
            IDLE,
            ACQUIRING,
            LOCKED,
            RELEASING,
            LOST,
            CLOSED
        }
    }
}
