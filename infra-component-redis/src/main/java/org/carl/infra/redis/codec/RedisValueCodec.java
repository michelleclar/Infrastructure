package org.carl.infra.redis.codec;

import com.fasterxml.jackson.core.type.TypeReference;

public interface RedisValueCodec {

    String encode(Object value);

    <T> T decode(String value, Class<T> type);

    <T> T decode(String value, TypeReference<T> type);
}
