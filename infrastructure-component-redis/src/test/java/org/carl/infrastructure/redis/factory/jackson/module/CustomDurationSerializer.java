package org.carl.infrastructure.redis.factory.jackson.module;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Duration;

// 自定义 Duration 序列化器
public class CustomDurationSerializer extends StdSerializer<Duration> {

    public CustomDurationSerializer() {
        super(Duration.class);
    }

    @Override
    public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeNumber(value.getSeconds());
    }
}
