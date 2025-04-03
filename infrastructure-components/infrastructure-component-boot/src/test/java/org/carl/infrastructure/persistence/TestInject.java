package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.core.PersistenceContext;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TestInject {
    @Inject PersistenceContext context;

    @Test
    void inject() {
        System.out.println(context);
    }
}
