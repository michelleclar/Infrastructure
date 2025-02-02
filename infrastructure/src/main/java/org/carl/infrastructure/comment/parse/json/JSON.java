package org.carl.infrastructure.comment.parse.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import org.carl.infrastructure.comment.parse.json.adapter.LocalDateAdapter;
import org.carl.infrastructure.comment.parse.json.adapter.LocalDateTimeAdapter;
import org.carl.infrastructure.comment.parse.json.adapter.ZonedDateTimeTypeAdapter;

public class JSON {
    static Gson gson;

    static {
        overload(builder -> {});
    }

    public static void overload(Consumer<GsonBuilder> f) {
        GsonBuilder gsonBuilder =
                new GsonBuilder()
                        .setPrettyPrinting()
                        //            .serializeNulls()
                        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter());
        f.accept(gsonBuilder);
        gson = gsonBuilder.create();
    }

    public static String toJsonString(Object obj) {
        return to(o -> o.toJson(obj));
    }

    public static JsonObject toJsonObject(Object obj) {
        return to(o -> o.toJsonTree(obj).getAsJsonObject());
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return to(o -> o.fromJson(json, clazz));
    }

    static <T> T to(Function<Gson, T> f) {
        return f.apply(gson);
    }
}
