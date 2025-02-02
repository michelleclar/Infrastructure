package org.carl.infrastructure.cache;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.carl.infrastructure.comment.Conversion;

@ApplicationScoped
@IfBuildProperty(name = "quarkus.remote.cache.enable", stringValue = "true")
public class RemoteCacheService {

    private final ReactiveKeyCommands<String> reactiveKeyCommands;
    private final KeyCommands<String> keyCommands;
    private final ReactiveValueCommands<String, Object> reactiveValueCommands;
    private final ValueCommands<String, Object> valueCommands;

    public RemoteCacheService(RedisDataSource ds, ReactiveRedisDataSource reactive) {
        this.reactiveKeyCommands = reactive.key();
        this.valueCommands = ds.value(Object.class);
        this.keyCommands = ds.key();
        this.reactiveValueCommands = reactive.value(Object.class);
    }

    public Conversion get(CacheTopic topic) {
        return this.executeValueCommands(o -> valueCommands.get(topic.cacheName));
    }

    public Uni<Void> del(String key) {
        return executeKeyCommands(o -> o.del(key).replaceWithVoid());
    }

    public Uni<Void> set(String key, Object value) {
        executeValueCommands(o -> o.setnx(key, value));
        return null;
    }

    public Boolean setExpireAfterWrite(String key, long seconds, Object value) {
        Uni<Boolean> uni =
                this.reactiveValueCommands
                        .setnx(key, value)
                        .onItem()
                        .call(
                                o ->
                                        this.reactiveKeyCommands.expire(
                                                key, Duration.ofSeconds(seconds)));
        boolean isSuccess = this.valueCommands.setnx(key, value);
        if (isSuccess) return this.keyCommands.expire(key, seconds);
        return false;
    }

    public Uni<Boolean> setExpireAfterWrite1(String key, long seconds, Object value) {
        return this.reactiveValueCommands
                .setnx(key, value)
                .onItem()
                .call(o -> this.reactiveKeyCommands.expire(key, Duration.ofSeconds(seconds)));
    }

    public Uni<List<String>> keys() {
        return executeKeyCommands(o -> o.keys("*"));
    }

    Conversion executeValueCommands(Function<ValueCommands<String, Object>, Object> f) {
        return Conversion.create(f.apply(this.valueCommands));
    }

    <T> T executeKeyCommands(Function<ReactiveKeyCommands<String>, T> f) {
        return f.apply(this.reactiveKeyCommands);
    }
}
