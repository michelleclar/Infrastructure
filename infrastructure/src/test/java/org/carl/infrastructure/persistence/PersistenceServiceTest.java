package org.carl.infrastructure.persistence;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.testcontainers.PostgresTestResource;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = PostgresTestResource.class)
class PersistenceServiceTest {
    @Inject PersistenceService persistenceService;

    @Test
    void save() {
        persistenceService.transaction(dsl -> {});
    }
}
