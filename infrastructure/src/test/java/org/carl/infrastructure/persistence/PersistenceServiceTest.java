package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistenceServiceTest {
    @Inject PersistenceService persistenceService;

    @Test
    void save() {
        persistenceService.transaction(dsl -> {});
    }
}
