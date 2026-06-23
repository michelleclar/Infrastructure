package org.carl.infrastructure.persistence.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * JDBC URL generation for external target database workflows.
 */
public final class ExternalJdbcUrls {
    private ExternalJdbcUrls() {}

    public static String build(ExternalDatabaseConfig config) {
        if (config == null) {
            throw new ExternalDatabaseException(
                    ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                    "External database config is required");
        }
        if (config.kind() == null) {
            throw new ExternalDatabaseException(
                    ExternalDatabaseFailureType.UNSUPPORTED_DATABASE_KIND,
                    "External database kind is required");
        }

        String baseUrl = config.jdbcUrlOverride();
        if (isBlank(baseUrl)) {
            if (isBlank(config.host())) {
                throw new ExternalDatabaseException(
                        ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                        "External database host is required when jdbcUrlOverride is not set");
            }
            if (isBlank(config.databaseName())) {
                throw new ExternalDatabaseException(
                        ExternalDatabaseFailureType.INVALID_CONFIGURATION,
                        "External database name is required when jdbcUrlOverride is not set");
            }
            int port = config.port() == null ? config.kind().getDefaultPort() : config.port();
            baseUrl =
                    "%s://%s:%d/%s"
                            .formatted(
                                    config.kind().getJdbcScheme(),
                                    config.host(),
                                    port,
                                    encode(config.databaseName()));
        }

        if (config.jdbcOptions().isEmpty()) {
            return baseUrl;
        }

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : config.jdbcOptions().entrySet()) {
            String key = encode(entry.getKey());
            String value = entry.getValue() == null ? "" : encode(entry.getValue());
            joiner.add(key + "=" + value);
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + joiner;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
