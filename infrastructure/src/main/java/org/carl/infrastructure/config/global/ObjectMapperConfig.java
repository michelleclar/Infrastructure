package org.carl.infrastructure.config.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.carl.infrastructure.util.parse.json.JacksonProvider;

@ApplicationScoped
public class ObjectMapperConfig {

    @Produces
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return JacksonProvider.JACKSON.get();
    }
}
