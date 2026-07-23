package org.carl.infra.redis.factory;

import com.fasterxml.jackson.databind.Module;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisClusterTransactions;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RedisConfigOptions {
    public static final Duration DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(30);

    private final RedisOptions internalOptions;
    private final List<Module> jacksonModules = new ArrayList<>();
    private Duration commandTimeout = DEFAULT_COMMAND_TIMEOUT;

    public RedisConfigOptions() {
        this.internalOptions = new RedisOptions();
    }

    public RedisConfigOptions setConnectionString(String connectionString) {
        this.internalOptions.setConnectionString(requireText(connectionString, "connectionString"));
        return this;
    }

    /**
     * @param type
     * @return
     */
    public RedisConfigOptions setConnectType(SentinelType type) {
        RedisClientType redisClientType =
                switch (Objects.requireNonNull(type, "type must not be null")) {
                    case REPLICATION -> RedisClientType.REPLICATION;
                    case SENTINEL -> RedisClientType.SENTINEL;
                    case STANDALONE -> RedisClientType.STANDALONE;
                    case CLUSTER -> RedisClientType.CLUSTER;
                };

        this.internalOptions.setType(redisClientType);
        return this;
    }

    public RedisConfigOptions setSentinelRole(SentinelRole role) {
        RedisRole redisRole =
                switch (Objects.requireNonNull(role, "role must not be null")) {
                    case MASTER -> RedisRole.MASTER;
                    case SENTINEL -> RedisRole.SENTINEL;
                    case REPLICA -> RedisRole.REPLICA;
                };
        this.internalOptions.setRole(redisRole);
        return this;
    }

    public RedisConfigOptions setSentinelMasterName(String masterName) {
        this.internalOptions.setMasterName(requireText(masterName, "masterName"));
        return this;
    }

    public RedisConfigOptions addConnectionString(String connect) {
        this.internalOptions.addConnectionString(requireText(connect, "connect"));
        return this;
    }

    @Deprecated(forRemoval = true)
    public RedisConfigOptions setDatabase(int database) {
        throw new UnsupportedOperationException(
                "setDatabase(int) is not supported; configure the database in the connection URI");
    }

    public RedisConfigOptions setPassword(String password) {
        this.internalOptions.setPassword(password);
        return this;
    }

    public RedisConfigOptions setMaxPoolSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        this.internalOptions.setMaxPoolSize(size);
        return this;
    }

    public RedisConfigOptions setMaxPoolWaiting(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
        this.internalOptions.setMaxPoolWaiting(size);
        return this;
    }

    public RedisConfigOptions setConnectTimeout(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be greater than 0");
        }
        if (this.internalOptions.getNetClientOptions() == null) {
            this.internalOptions.setNetClientOptions(new NetClientOptions());
        }
        this.internalOptions.getNetClientOptions().setConnectTimeout(timeout);
        return this;
    }

    public RedisConfigOptions setNetClientOptions(NetClientOptions options) {
        this.internalOptions.setNetClientOptions(
                Objects.requireNonNull(options, "options must not be null"));
        return this;
    }

    public RedisConfigOptions setMaxWaitingHandlers(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        this.internalOptions.setMaxWaitingHandlers(size);
        return this;
    }

    public RedisConfigOptions setSentinelAutoFailover(boolean autoFailover) {
        this.internalOptions.setAutoFailover(autoFailover);
        return this;
    }

    public RedisConfigOptions setUseReplicas(RedisReplicas useReplicas) {
        this.internalOptions.setUseReplicas(
                Objects.requireNonNull(useReplicas, "useReplicas must not be null"));
        return this;
    }

    public RedisConfigOptions setClusterTransactions(
            RedisClusterTransactions clusterTransactions) {
        this.internalOptions.setClusterTransactions(
                Objects.requireNonNull(
                        clusterTransactions, "clusterTransactions must not be null"));
        return this;
    }

    public RedisConfigOptions setTracingPolicy(TracingPolicy tracingPolicy) {
        this.internalOptions.setTracingPolicy(
                Objects.requireNonNull(tracingPolicy, "tracingPolicy must not be null"));
        return this;
    }

    public RedisConfigOptions setPoolName(String poolName) {
        this.internalOptions.setPoolName(requireText(poolName, "poolName"));
        return this;
    }

    public RedisConfigOptions setMetricsName(String metricsName) {
        this.internalOptions.setMetricsName(requireText(metricsName, "metricsName"));
        return this;
    }

    public RedisConfigOptions setCommandTimeout(Duration timeout) {
        this.commandTimeout = requirePositiveDuration(timeout, "timeout");
        return this;
    }

    public RedisConfigOptions registerModules(Module module) {
        this.jacksonModules.add(Objects.requireNonNull(module, "module must not be null"));
        return this;
    }

    List<Module> getJacksonModules() {
        return List.copyOf(jacksonModules);
    }

    RedisOptions getActualOptions() {
        return new RedisOptions(this.internalOptions);
    }

    Duration getCommandTimeout() {
        return commandTimeout;
    }

    private static Duration requirePositiveDuration(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
        try {
            if (duration.toMillis() <= 0) {
                throw new IllegalArgumentException(name + " must be at least 1 millisecond");
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " is too large", e);
        }
        return duration;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
