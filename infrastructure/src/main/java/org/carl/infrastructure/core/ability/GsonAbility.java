package org.carl.infrastructure.core.ability;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.function.Function;
import org.carl.infrastructure.util.parse.json.GsonProvider;

public interface GsonAbility {

    default String toJsonString(Object obj) {
        return run(o -> o.toJson(obj));
    }

    default JsonObject toJsonObject(Object obj) {
        return run(o -> o.toJsonTree(obj).getAsJsonObject());
    }

    default <T> T fromJson(String json, Class<T> clazz) {
        return run(o -> o.fromJson(json, clazz));
    }

    default <T> T run(Function<Gson, T> f) {
        return f.apply(GsonProvider.GSON.get());
    }
}
