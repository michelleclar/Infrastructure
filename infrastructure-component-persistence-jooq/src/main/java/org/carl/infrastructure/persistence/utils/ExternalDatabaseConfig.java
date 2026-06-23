package org.carl.infrastructure.persistence.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/**
 * Neutral external database connection configuration.
 */
public record ExternalDatabaseConfig(
        ExternalDatabaseKind kind,
        String host,
        Integer port,
        String databaseName,
        String username,
        String password,
        String schema,
        String jdbcUrlOverride,
        Map<String, String> jdbcOptions) {

    public ExternalDatabaseConfig {
        if (jdbcOptions == null) {
            jdbcOptions = Map.of();
        } else {
            jdbcOptions = Collections.unmodifiableMap(new LinkedHashMap<>(jdbcOptions));
        }
    }

    public static Builder builder(ExternalDatabaseKind kind) {
        return new Builder(kind);
    }

    public static final class Builder {
        private final ExternalDatabaseKind kind;
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private String password;
        private String schema;
        private String jdbcUrlOverride;
        private final Map<String, String> jdbcOptions = new LinkedHashMap<>();

        private Builder(ExternalDatabaseKind kind) {
            this.kind = kind;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder jdbcUrlOverride(String jdbcUrlOverride) {
            this.jdbcUrlOverride = jdbcUrlOverride;
            return this;
        }

        public Builder jdbcOption(String key, String value) {
            this.jdbcOptions.put(Objects.requireNonNull(key), value);
            return this;
        }

        public Builder jdbcOptions(Map<String, String> options) {
            if (options != null) {
                this.jdbcOptions.putAll(options);
            }
            return this;
        }

        public ExternalDatabaseConfig build() {
            return new ExternalDatabaseConfig(
                    kind,
                    host,
                    port,
                    databaseName,
                    username,
                    password,
                    schema,
                    jdbcUrlOverride,
                    jdbcOptions);
        }
    }
}
