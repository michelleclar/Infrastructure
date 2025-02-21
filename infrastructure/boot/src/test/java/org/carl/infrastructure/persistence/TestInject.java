package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TestInject {
    @Inject PersistenceService provider;

    @Test
    void inject() {
        System.out.println(provider.getPersistenceContext().configuration());
    }
}
