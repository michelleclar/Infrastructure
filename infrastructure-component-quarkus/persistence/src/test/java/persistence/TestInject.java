package persistence;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "JDBC_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JDBC_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JDBC_PASSWORD", matches = ".+")
class TestInject {
    @Inject PersistenceContext context;

    @Test
    void inject() {
        System.out.println(context);
    }
}
