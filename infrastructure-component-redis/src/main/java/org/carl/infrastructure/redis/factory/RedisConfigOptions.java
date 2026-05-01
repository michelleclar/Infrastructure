package org.carl.infrastructure.redis.factory;

import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;

public class RedisConfigOptions {
    private final RedisOptions internalOptions;

    public RedisConfigOptions() {
        this.internalOptions = new RedisOptions();
    }

    public RedisConfigOptions setConnectionString(String connectionString) {
        this.internalOptions.setConnectionString(connectionString);
        return this;
    }

    /**
     * @param type
     * @return
     */
    public RedisConfigOptions setConnectType(SentinelType type) {
        RedisClientType redisClientType =
                switch (type) {
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
                switch (role) {
                    case MASTER -> RedisRole.MASTER;
                    case SENTINEL -> RedisRole.SENTINEL;
                    case REPLICA -> RedisRole.REPLICA;
                };
        this.internalOptions.setRole(redisRole);
        return this;
    }

    public RedisConfigOptions setSentinelMasterName(String masterName) {
        this.internalOptions.setMasterName(masterName);
        return this;
    }

    public RedisConfigOptions addConnectionString(String connect) {
        this.internalOptions.addConnectionString(connect);
        return this;
    }

    public RedisConfigOptions setDatabase(int database) {
        // 如果需要单独设置，通常需要自定义逻辑或确保 URI 正确
        return this;
    }

    public RedisConfigOptions setPassword(String password) {
        this.internalOptions.setPassword(password);
        return this;
    }

    public RedisConfigOptions setMaxPoolSize(int size) {
        this.internalOptions.setMaxPoolSize(size);
        return this;
    }

    public RedisConfigOptions setMaxPoolWaiting(int size) {
        this.internalOptions.setMaxPoolWaiting(size);
        return this;
    }

    public RedisConfigOptions setConnectTimeout(int timeout) {
        if (this.internalOptions.getNetClientOptions() == null) {
            this.internalOptions.setNetClientOptions(new NetClientOptions());
        }
        this.internalOptions.getNetClientOptions().setConnectTimeout(timeout);
        return this;
    }

    public void registerModules(com.fasterxml.jackson.databind.Module module) {
        io.vertx.core.json.jackson.DatabindCodec.mapper().registerModule(module);
    }

    RedisOptions getActualOptions() {
        return this.internalOptions;
    }
}
