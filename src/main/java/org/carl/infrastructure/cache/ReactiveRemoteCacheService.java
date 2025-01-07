package org.carl.infrastructure.cache;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.function.Function;

// TODO: need redisAPI override,because is base vert.x
@ApplicationScoped
public class ReactiveRemoteCacheService implements BaseCache {

    private final ReactiveKeyCommands<String> reactiveKeyCommands;
    private final ReactiveValueCommands<String, Object> reactiveValueCommands;

    public ReactiveRemoteCacheService(ReactiveRedisDataSource reactive) {
        this.reactiveKeyCommands = reactive.key();
        this.reactiveValueCommands = reactive.value(Object.class);
    }

    @Override
    public Uni<Object> get(String key) {
        return executeValueCommands(o -> o.get(key));
    }

    @Override
    public Uni<Void> del(String key) {
        return executeKeyCommands(o -> o.del(key).replaceWithVoid());
    }

    @Override
    public Uni<Void> set(String key, Object value) {
        return executeValueCommands(o -> o.set(key, value)).replaceWithVoid();
    }

    public Uni<Boolean> setExpireAfterWrite1(String key, long seconds, Object value) {
        return executeValueCommands(o -> o.setnx(key, value))
                .onItem()
                .call(
                        o -> {
                            if (!o) return Uni.createFrom().item(false);
                            return executeKeyCommands(
                                    keyCommands -> keyCommands.expire(key, seconds));
                        });
    }

    @Override
    public Uni<List<String>> keys() {
        return executeKeyCommands(o -> o.keys("*"));
    }

    <T> T executeValueCommands(Function<ReactiveValueCommands<String, Object>, T> f) {
        return f.apply(this.reactiveValueCommands);
    }

    <T> T executeKeyCommands(Function<ReactiveKeyCommands<String>, T> f) {
        return f.apply(this.reactiveKeyCommands);
    }
}
