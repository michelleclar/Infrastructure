package org.carl.infrastructure.cache;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;
import org.carl.infrastructure.comment.Conversion;

@ApplicationScoped
public class RemoteCacheService implements BaseCache {

    private final ReactiveKeyCommands<String> reactiveKeyCommands;
    private final KeyCommands<String> keyCommands;
    private final ValueCommands<String, Object> valueCommands;

    public RemoteCacheService(RedisDataSource ds, ReactiveRedisDataSource reactive) {
        reactiveKeyCommands = reactive.key();
        this.valueCommands = ds.value(Object.class);
        this.keyCommands = ds.key();
    }

    @Override
    public Conversion get(String key) {
        return this.executeValueCommands(o -> valueCommands.get(key));
    }

    @Override
    public Uni<Void> del(String key) {
        return executeKeyCommands(o -> o.del(key).replaceWithVoid());
    }

    @Override
    public Uni<Boolean> put(String key, Object value) {
        return executeValueCommands(o -> o.setnx(key, value)).toBoolean();
    }

    public Boolean setExpireAfterWrite(String key, long seconds, Object value) {

        boolean isSuccess = this.valueCommands.setnx(key, value);
        if (isSuccess) return this.keyCommands.expire(key, seconds);
        return false;
    }

    @Override
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
