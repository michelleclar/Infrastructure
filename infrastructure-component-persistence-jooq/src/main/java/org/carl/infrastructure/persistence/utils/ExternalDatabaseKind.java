package org.carl.infrastructure.persistence.utils;

/**
 * External database engines supported by the low-frequency utility APIs.
 */
public enum ExternalDatabaseKind {
    POSTGRESQL("jdbc:postgresql", 5432),
    MYSQL("jdbc:mysql", 3306);

    private final String jdbcScheme;
    private final int defaultPort;

    ExternalDatabaseKind(String jdbcScheme, int defaultPort) {
        this.jdbcScheme = jdbcScheme;
        this.defaultPort = defaultPort;
    }

    public String getJdbcScheme() {
        return jdbcScheme;
    }

    public int getDefaultPort() {
        return defaultPort;
    }
}
