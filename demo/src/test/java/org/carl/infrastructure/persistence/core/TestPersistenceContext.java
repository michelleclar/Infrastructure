package org.carl.infrastructure.persistence.core;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class TestPersistenceContext {
    @Inject PersistenceContext dsl;

    @Test
    public void testPersistenceContext() {
        System.out.println(dsl);
    }
}
