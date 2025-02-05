package org.carl.infrastructure.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String POSTGRES_IMAGE = "postgres:17-alpine";

    private static final JdbcDatabaseContainer POSTGRES =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withLogConsumer(outputFrame -> {})
                    .withReuse(true);

    @Override
    public Map<String, String> start() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
        return ImmutableMap.of(
                "quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl(),
                "quarkus.datasource.username", POSTGRES.getUsername(),
                "quarkus.datasource.password", POSTGRES.getPassword());
    }

    @Override
    public void stop() {}
}
