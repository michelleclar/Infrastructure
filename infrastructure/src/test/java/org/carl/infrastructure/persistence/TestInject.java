package org.carl.infrastructure.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.carl.infrastructure.persistence.database.core.PersistenceProvider;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TestInject {
    @Inject PersistenceProvider provider;

    @Test
    void inject() {
        System.out.println(provider.getDSLContext().configuration());
    }
}
