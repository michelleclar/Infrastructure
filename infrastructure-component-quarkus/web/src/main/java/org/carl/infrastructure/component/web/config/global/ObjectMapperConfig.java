package org.carl.infrastructure.component.web.config.global;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.All;
import io.quarkus.jackson.ObjectMapperCustomizer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.carl.infrastructure.component.web.config.JacksonProvider;

import java.util.List;

@ApplicationScoped
public class ObjectMapperConfig {

    @Singleton
    @Produces
    public ObjectMapper objectMapper(@All List<ObjectMapperCustomizer> customizers) {
        ObjectMapper objectMapper = JacksonProvider.JACKSON.get();
        customizers.forEach(customizer -> customizer.customize(objectMapper));
        return objectMapper;
    }
}
