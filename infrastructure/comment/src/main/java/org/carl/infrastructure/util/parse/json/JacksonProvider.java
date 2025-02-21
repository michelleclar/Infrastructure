package org.carl.infrastructure.util.parse.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import jakarta.inject.Provider;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Consumer;

public enum JacksonProvider implements Provider<ObjectMapper> {
    JACKSON;
    final ObjectMapper objectMapper;

    JacksonProvider() {
        this.objectMapper =
                get(
                        jackson -> {
                            SimpleModule customModule = new SimpleModule();
                            customModule.addSerializer(Date.class, new CustomDateSerializer());
                            customModule.addSerializer(
                                    LocalDateTime.class, new CustomLocalDateTimeSerializer());
                            customModule.addSerializer(
                                    Duration.class, new CustomDurationSerializer());
                            jackson.registerModule(customModule);
                        });
    }

    public ObjectMapper get(Consumer<ObjectMapper> f) {
        ObjectMapper objectMapper = new ObjectMapper();
        f.accept(objectMapper);
        return objectMapper;
    }

    @Override
    public ObjectMapper get() {
        return this.objectMapper;
    }
}

// 自定义 Date 序列化器
class CustomDateSerializer extends StdSerializer<Date> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat CALENDAR_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public CustomDateSerializer() {
        this(null);
    }

    public CustomDateSerializer(Class<Date> t) {
        super(t);
    }

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value instanceof java.sql.Date) {
            gen.writeString(CALENDAR_FORMAT.format(value));
        } else {
            gen.writeString(DATE_FORMAT.format(value));
        }
    }
}

// 自定义 LocalDateTime 序列化器
class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CustomLocalDateTimeSerializer() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeString(value.format(FORMATTER));
    }
}

// 自定义 Duration 序列化器
class CustomDurationSerializer extends StdSerializer<Duration> {

    public CustomDurationSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeNumber(value.getSeconds());
    }
}
