package org.carl.infrastructure.workflow.config;

import io.smallrye.config.PropertiesConfigSource;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import java.util.List;
import java.util.Properties;

@ApplicationScoped
public class ModuleConfigSourceProvider implements ConfigSourceProvider {
    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        Properties properties = new Properties();
        return List.of(new PropertiesConfigSource(properties, "workflow", 250));
    }
}
