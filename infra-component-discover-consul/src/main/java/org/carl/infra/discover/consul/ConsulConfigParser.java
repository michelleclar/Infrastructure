package org.carl.infra.discover.consul;

import org.carl.infra.discover.DynamicConfigSnapshot;
import org.carl.infra.discover.DynamicConfigValidationException;
import org.carl.infra.discover.DynamicConfigValidator;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

final class ConsulConfigParser {

    private ConsulConfigParser() {}

    static DynamicConfigSnapshot parse(
            String content, String version, List<DynamicConfigValidator> validators) {
        if (content == null) {
            throw new DynamicConfigValidationException("Configuration content must not be null");
        }

        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException | IllegalArgumentException error) {
            throw new DynamicConfigValidationException(
                    "Configuration is not a valid properties document", error);
        }

        Map<String, String> values = new TreeMap<>();
        properties.forEach(
                (key, value) -> values.put(String.valueOf(key), String.valueOf(value)));
        DynamicConfigSnapshot snapshot =
                new DynamicConfigSnapshot(version, Instant.now(), values);
        for (DynamicConfigValidator validator : validators) {
            try {
                validator.validate(snapshot);
            } catch (RuntimeException error) {
                throw new DynamicConfigValidationException(
                        "Dynamic configuration validation failed", error);
            }
        }
        return snapshot;
    }
}
