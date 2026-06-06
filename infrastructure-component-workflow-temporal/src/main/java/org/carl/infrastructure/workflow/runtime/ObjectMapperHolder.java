package org.carl.infrastructure.workflow.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Static holder for a process-wide {@link ObjectMapper} used inside workflow code to convert {@link
 * com.fasterxml.jackson.databind.JsonNode} configuration into typed handler config records.
 *
 * <p>Defaults to a vanilla {@link ObjectMapper}. Callers may inject a customised mapper via {@link
 * #install(ObjectMapper)} before starting the worker.
 */
public final class ObjectMapperHolder {

    private static volatile ObjectMapper MAPPER = new ObjectMapper();

    private ObjectMapperHolder() {
        throw new AssertionError("no instances");
    }

    public static void install(ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper must not be null");
        }
        MAPPER = mapper;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
