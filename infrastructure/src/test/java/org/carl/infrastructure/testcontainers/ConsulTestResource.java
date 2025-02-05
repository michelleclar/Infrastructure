package org.carl.infrastructure.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.junit.ClassRule;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ConsulTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String TAG = "hashicorp/consul:1.20";
    @ClassRule
    public static ConsulContainer CONSUL = new ConsulContainer(TAG)
            .withConsulCommand("kv put config/testing1 value123");


    @Override
    public Map<String, String> start() {
        if (!CONSUL.isRunning()) {
            CONSUL.start();
        }
        return ImmutableMap.of(
                "quarkus.datasource.jdbc.url", CONSUL.getHost());
    }

    @Override
    public void stop() {
    }
}
