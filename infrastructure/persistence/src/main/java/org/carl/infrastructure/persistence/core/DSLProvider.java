package org.carl.infrastructure.persistence.core;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class DSLProvider {
    @Produces
    @ApplicationScoped
    public Dsl createDsl() {
        return new Dsl();
    }
}
