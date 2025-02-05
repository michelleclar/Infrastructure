package org.carl.infrastructure.util.parse.json;

import com.google.gson.*;
import jakarta.inject.Provider;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public enum GsonProvider implements Provider<Gson> {
    GSON;
    final Gson gson;

    GsonProvider() {
        this.gson = get(builder -> {
        });
    }

    public Gson get(Consumer<GsonBuilder> f) {
        GsonBuilder gsonBuilder =
                new GsonBuilder()
                        .setPrettyPrinting()
                        //            .serializeNulls()
                        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter());
        f.accept(gsonBuilder);
        return gsonBuilder.create();
    }

    @Override
    public Gson get() {
        return this.gson;
    }
}

class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public JsonElement serialize(
            LocalDate localDate, Type type, JsonSerializationContext jsonSerializationContext) {

        return new JsonPrimitive(localDate.format(formatter)); // "yyyy-MM-dd"
    }

    @Override
    public LocalDate deserialize(
            JsonElement jsonElement,
            Type type,
            JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {

        return LocalDate.parse(jsonElement.getAsJsonPrimitive().getAsString(), formatter);
    }
}

class LocalDateTimeAdapter
        implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public JsonElement serialize(
            LocalDateTime localDateTime,
            Type type,
            JsonSerializationContext jsonSerializationContext) {

        return new JsonPrimitive(localDateTime.format(formatter)); // "yyyy-MM-dd"
    }

    @Override
    public LocalDateTime deserialize(
            JsonElement jsonElement,
            Type type,
            JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        return LocalDateTime.parse(jsonElement.getAsString(), formatter);
    }
}

class ZonedDateTimeTypeAdapter
        implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss z");

    @Override
    public JsonElement serialize(
            ZonedDateTime zonedDateTime, Type srcType, JsonSerializationContext context) {

        return new JsonPrimitive(formatter.format(zonedDateTime));
    }

    @Override
    public ZonedDateTime deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        return ZonedDateTime.parse(json.getAsString(), formatter);
    }
}
