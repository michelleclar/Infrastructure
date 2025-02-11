package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.database.core.PersistenceStd;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TestInject {
    @Inject PersistenceStd provider;

    @Test
    void inject() {
        System.out.println(provider.getPersistenceContext().configuration());
    }
}
