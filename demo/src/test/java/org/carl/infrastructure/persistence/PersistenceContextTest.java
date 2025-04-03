package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistenceContextTest {
    @Inject PersistenceContext persistenceContext;

    @Test
    public void testInsert() {
        System.out.println(persistenceContext);
    }
}
