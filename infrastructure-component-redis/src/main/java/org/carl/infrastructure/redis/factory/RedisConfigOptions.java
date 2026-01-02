package org.carl.infrastructure.redis.factory;

import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.RedisOptions;

public class RedisConfigOptions {
    private final RedisOptions internalOptions;

    public RedisConfigOptions() {
        this.internalOptions = new RedisOptions();
    }

    public RedisConfigOptions setConnectionString(String connectionString) {
        this.internalOptions.setConnectionString(connectionString);
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
