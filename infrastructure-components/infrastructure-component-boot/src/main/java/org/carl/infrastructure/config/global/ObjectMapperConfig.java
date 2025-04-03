package org.carl.infrastructure.config.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.All;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;
import org.carl.infrastructure.util.parse.json.JacksonProvider;

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
